package com.example.thoughts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

private const val DraftIdKey = "draft_id"
private const val RecordingSessionIdKey = "draft_recording_session_id"
private const val TitleKey = "draft_title"
private const val TranscriptTextKey = "draft_transcript_text"
private const val TagsKey = "draft_tags"
private const val MoodLabelKey = "draft_mood_label"
private const val MoodScoreKey = "draft_mood_score"
private const val MoodExplanationKey = "draft_mood_explanation"
private const val TakeawayKey = "draft_takeaway"
private const val UpdatedAtKey = "draft_updated_at"
private const val RecordingIdKey = "recording_id"
private const val RecordingStartedAtKey = "recording_started_at"
private const val RecordingEndedAtKey = "recording_ended_at"
private const val RecordingDurationKey = "recording_duration"
private const val RecordingStatusKey = "recording_status"
private const val TAG = "JournalViewModel"
private const val MAX_UPLOAD_RETRIES = 3

class JournalViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private val _currentDraft = MutableStateFlow(readDraftFromState())
    val currentDraft: StateFlow<JournalEntryDraft?> = _currentDraft.asStateFlow()

    private val _recordingSession = MutableStateFlow(readRecordingSessionFromState())
    val recordingSession: StateFlow<RecordingSession> = _recordingSession.asStateFlow()

    private val _uploadState = MutableStateFlow(AudioUploadState.Local)
    val uploadState: StateFlow<AudioUploadState> = _uploadState.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _backendResult = MutableStateFlow<IngestionResponse?>(null)
    val backendResult: StateFlow<IngestionResponse?> = _backendResult.asStateFlow()

    private val _selectedEntry = MutableStateFlow<JournalEntry?>(null)
    val selectedEntry: StateFlow<JournalEntry?> = _selectedEntry.asStateFlow()

    private var timerJob: Job? = null
    private var audioFile: File? = null
    private var audioRecorder: AudioRecorder? = null
    private var uploadRetryCount = 0

    private val _archivedEntries = MutableStateFlow<List<ArchiveEntrySummary>>(emptyList())
    val archivedEntries: StateFlow<List<ArchiveEntrySummary>> = _archivedEntries.asStateFlow()

    fun saveDraft(draft: JournalEntryDraft) {
        _currentDraft.value = draft
        persistDraft(draft)
    }

    fun ensureDraftInitialized() {
        if (_currentDraft.value == null) {
            val now = System.currentTimeMillis()
            saveDraft(createDefaultDraft(now))
        }
    }

    fun beginRecording() {
        val now = System.currentTimeMillis()
        val session = RecordingSession(
            id = savedStateHandle.get<String>(RecordingIdKey) ?: "session-$now",
            startedAtMillis = savedStateHandle.get<Long>(RecordingStartedAtKey) ?: now,
            endedAtMillis = null,
            durationMs = 0L,
            status = RecordingStatus.Recording,
        )
        _recordingSession.value = session
        persistRecordingSession(session)
        ensureDraftInitialized()

        // Create audio file and start recording
        audioFile = AudioFileManager.createAudioFile(getApplication())
        audioRecorder = AudioRecorder(getApplication()).apply {
            start(audioFile!!)
        }
        Log.d(TAG, "Started recording to: ${audioFile?.absolutePath}")

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _recordingSession.value
                if (current.status == RecordingStatus.Recording) {
                    val updated = current.copy(durationMs = current.durationMs + 1000)
                    _recordingSession.value = updated
                    persistRecordingSession(updated)
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun setRecordingStatus(status: RecordingStatus) {
        val now = System.currentTimeMillis()
        val current = _recordingSession.value
        val updatedSession = when (status) {
            RecordingStatus.Paused -> {
                audioRecorder?.pause()
                current.copy(status = status)
            }
            RecordingStatus.Recording -> {
                audioRecorder?.resume()
                current.copy(status = status, endedAtMillis = null)
            }
            RecordingStatus.Finished -> {
                audioRecorder?.stop()
                stopTimer()
                current.copy(
                    status = status,
                    endedAtMillis = now,
                    durationMs = now - current.startedAtMillis,
                )
            }
            RecordingStatus.Discarded -> {
                audioRecorder?.stop()
                stopTimer()
                current.copy(status = status, endedAtMillis = now)
            }
            RecordingStatus.Error -> {
                audioRecorder?.stop()
                current.copy(status = status)
            }
            RecordingStatus.Idle -> {
                audioRecorder?.stop()
                current.copy(status = status)
            }
        }
        _recordingSession.value = updatedSession
        persistRecordingSession(updatedSession)
    }

    fun finishRecordingAndUpload() {
        audioRecorder?.stop()
        stopTimer()

        val now = System.currentTimeMillis()
        val updated = _recordingSession.value.copy(
            status = RecordingStatus.Finished,
            endedAtMillis = now,
            durationMs = now - _recordingSession.value.startedAtMillis,
        )
        _recordingSession.value = updated
        persistRecordingSession(updated)

        // Start upload
        uploadRetryCount = 0
        uploadAudioToBackend()
    }

    fun discardRecording() {
        audioRecorder?.stop()
        stopTimer()

        // Delete audio file
        audioFile?.let { AudioFileManager.deleteAudioFile(it) }
        audioFile = null

        // Reset state
        _recordingSession.value = RecordingSession(
            id = "",
            startedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun uploadAudioToBackend() {
        val file = audioFile ?: run {
            Log.e(TAG, "Audio file is null")
            _uploadError.value = "No audio file to upload"
            _uploadState.value = AudioUploadState.Failed
            return
        }

        _uploadState.value = AudioUploadState.Uploading
        _uploadError.value = null

        viewModelScope.launch {
            val durationMs = _recordingSession.value.durationMs
            val locale = Locale.getDefault().toLanguageTag()

            val result = BackendService.uploadAudioForTranscription(
                audioFile = file,
                durationMs = durationMs,
                locale = locale,
            )

            result.onSuccess { response ->
                Log.d(TAG, "Upload successful: recording=${response.recordingId}, entry=${response.entryId}")
                _uploadState.value = AudioUploadState.Processing
                val entryId = response.entryId
                if (!entryId.isNullOrBlank()) {
                    loadEntryFromBackend(entryId, response)
                } else {
                    processingFailed("Upload completed but the backend did not return an entry id.")
                }
            }

            result.onFailure { exception ->
                Log.e(TAG, "Upload failed", exception)
                val isPermanentFailure = exception is IllegalStateException || exception is IllegalArgumentException
                if (!isPermanentFailure && uploadRetryCount < MAX_UPLOAD_RETRIES) {
                    uploadRetryCount++
                    val backoffMs = (1000L * uploadRetryCount).coerceAtMost(5000L)
                    Log.d(TAG, "Retrying upload in ${backoffMs}ms (attempt $uploadRetryCount/$MAX_UPLOAD_RETRIES)")
                    delay(backoffMs)
                    uploadAudioToBackend()
                } else {
                    _uploadState.value = AudioUploadState.Failed
                    _uploadError.value = exception.message ?: "Upload failed"
                }
            }
        }
    }

    fun retryUpload() {
        uploadRetryCount = 0
        uploadAudioToBackend()
    }

    fun loadEntry(entryId: String) {
        viewModelScope.launch {
            BackendService.fetchEntry(entryId)
                .onSuccess { entry ->
                    _selectedEntry.value = entry
                }
                .onFailure { throwable ->
                    Log.e(TAG, "Failed to load entry $entryId", throwable)
                    _selectedEntry.value = null
                }
        }
    }

    fun loadArchivedEntries() {
        viewModelScope.launch {
            BackendService.fetchArchivedEntries()
                .onSuccess { entries ->
                    _archivedEntries.value = entries
                }
                .onFailure { throwable ->
                    Log.e(TAG, "Failed to load archive entries", throwable)
                    _archivedEntries.value = emptyList()
                }
        }
    }

    fun listArchivedEntries(): List<ArchiveEntrySummary> = archivedEntries.value

    private fun loadEntryFromBackend(entryId: String, response: RecordingUploadResponse) {
        viewModelScope.launch {
            BackendService.fetchEntry(entryId)
                .onSuccess { entry ->
                    _backendResult.value = IngestionResponse(
                        id = entry.id,
                        recordingId = response.recordingId,
                        status = response.status,
                        progressPercent = response.progressPercent,
                        errorMessage = response.errorMessage,
                        entryId = entry.id,
                        draftId = response.draftId,
                        transcript = entry.transcript.fullText,
                        audioSignedUrl = entry.audioAsset?.remoteUrl,
                        createdAt = System.currentTimeMillis().toString(),
                    )
                    _selectedEntry.value = entry

                    val updatedDraft = JournalEntryDraft(
                        id = response.draftId ?: entry.id,
                        recordingSessionId = entry.recordingSessionId,
                        title = entry.title,
                        transcriptText = entry.transcript.fullText,
                        audioAsset = entry.audioAsset,
                        tags = entry.tags,
                        moodAnalysis = entry.moodAnalysis,
                        takeaway = entry.takeaway,
                        updatedAtMillis = System.currentTimeMillis(),
                    )
                    saveDraft(updatedDraft)

                    _uploadState.value = AudioUploadState.Uploaded
                    audioFile?.let { AudioFileManager.deleteAudioFile(it) }
                    audioFile = null
                }
                .onFailure { throwable ->
                    Log.e(TAG, "Failed to load uploaded entry $entryId", throwable)
                    processingFailed(throwable.message ?: "Could not load the uploaded journal entry.")
                }
        }
    }

    private fun processingFailed(message: String) {
        _uploadState.value = AudioUploadState.Failed
        _uploadError.value = message
    }

    fun updateTranscriptText(text: String) {
        val draft = currentDraftOrDefault().copy(
            transcriptText = text,
            updatedAtMillis = System.currentTimeMillis(),
        )
        saveDraft(draft)
    }

    fun updateTitle(title: String?) {
        val draft = currentDraftOrDefault().copy(
            title = title,
            updatedAtMillis = System.currentTimeMillis(),
        )
        saveDraft(draft)
    }

    fun updateTakeaway(takeaway: String?) {
        val draft = currentDraftOrDefault().copy(
            takeaway = takeaway,
            updatedAtMillis = System.currentTimeMillis(),
        )
        saveDraft(draft)
    }

    fun clearDraft() {
        _currentDraft.value = null
        savedStateHandle.remove<String>(DraftIdKey)
        savedStateHandle.remove<String>(RecordingSessionIdKey)
        savedStateHandle.remove<String>(TitleKey)
        savedStateHandle.remove<String>(TranscriptTextKey)
        savedStateHandle.remove<String>(TagsKey)
        savedStateHandle.remove<String>(MoodLabelKey)
        savedStateHandle.remove<Float>(MoodScoreKey)
        savedStateHandle.remove<String>(MoodExplanationKey)
        savedStateHandle.remove<String>(TakeawayKey)
        savedStateHandle.remove<Long>(UpdatedAtKey)
        _recordingSession.value = RecordingSession(
            id = "",
            startedAtMillis = System.currentTimeMillis(),
        )
        savedStateHandle.remove<String>(RecordingIdKey)
        savedStateHandle.remove<Long>(RecordingStartedAtKey)
        savedStateHandle.remove<Long>(RecordingEndedAtKey)
        savedStateHandle.remove<Long>(RecordingDurationKey)
        savedStateHandle.remove<String>(RecordingStatusKey)
    }

    private fun readDraftFromState(): JournalEntryDraft? {
        val draftId = savedStateHandle.get<String>(DraftIdKey) ?: return null
        val recordingSessionId = savedStateHandle.get<String>(RecordingSessionIdKey) ?: return null
        val transcriptText = savedStateHandle.get<String>(TranscriptTextKey) ?: return null
        val updatedAtMillis = savedStateHandle.get<Long>(UpdatedAtKey) ?: System.currentTimeMillis()
        val tags = savedStateHandle.get<String>(TagsKey)
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?.map { JournalTag(name = it) }
            ?: emptyList()
        val moodLabel = savedStateHandle.get<String>(MoodLabelKey)
        val moodScore = savedStateHandle.get<Float>(MoodScoreKey)
        val moodExplanation = savedStateHandle.get<String>(MoodExplanationKey)

        return JournalEntryDraft(
            id = draftId,
            recordingSessionId = recordingSessionId,
            title = savedStateHandle.get<String>(TitleKey),
            transcriptText = transcriptText,
            tags = tags,
            moodAnalysis = moodLabel?.let {
                MoodAnalysis(
                    label = it,
                    score = moodScore ?: 0f,
                    explanation = moodExplanation,
                )
            },
            takeaway = savedStateHandle.get<String>(TakeawayKey),
            updatedAtMillis = updatedAtMillis,
        )
    }

    private fun persistDraft(draft: JournalEntryDraft) {
        savedStateHandle[DraftIdKey] = draft.id
        savedStateHandle[RecordingSessionIdKey] = draft.recordingSessionId
        savedStateHandle[TitleKey] = draft.title
        savedStateHandle[TranscriptTextKey] = draft.transcriptText
        savedStateHandle[TagsKey] = draft.tags.joinToString("|") { it.name }
        savedStateHandle[MoodLabelKey] = draft.moodAnalysis?.label
        savedStateHandle[MoodScoreKey] = draft.moodAnalysis?.score
        savedStateHandle[MoodExplanationKey] = draft.moodAnalysis?.explanation
        savedStateHandle[TakeawayKey] = draft.takeaway
        savedStateHandle[UpdatedAtKey] = draft.updatedAtMillis
    }

    private fun readRecordingSessionFromState(): RecordingSession {
        val id = savedStateHandle.get<String>(RecordingIdKey) ?: ""
        val startedAtMillis = savedStateHandle.get<Long>(RecordingStartedAtKey) ?: System.currentTimeMillis()
        val endedAtMillis = savedStateHandle.get<Long>(RecordingEndedAtKey)
        val durationMs = savedStateHandle.get<Long>(RecordingDurationKey) ?: 0L
        val status = savedStateHandle.get<String>(RecordingStatusKey)
            ?.let { runCatching { RecordingStatus.valueOf(it) }.getOrNull() }
            ?: RecordingStatus.Idle

        return RecordingSession(
            id = id,
            startedAtMillis = startedAtMillis,
            endedAtMillis = endedAtMillis,
            durationMs = durationMs,
            status = status,
        )
    }

    private fun persistRecordingSession(session: RecordingSession) {
        savedStateHandle[RecordingIdKey] = session.id
        savedStateHandle[RecordingStartedAtKey] = session.startedAtMillis
        savedStateHandle[RecordingEndedAtKey] = session.endedAtMillis
        savedStateHandle[RecordingDurationKey] = session.durationMs
        savedStateHandle[RecordingStatusKey] = session.status.name
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder?.stop()
        stopTimer()
    }
}

fun createDefaultDraft(now: Long): JournalEntryDraft {
    val id = "draft-$now"
    return JournalEntryDraft(
        id = id,
        recordingSessionId = "session-$now",
        title = null,
        transcriptText = "",
        audioAsset = null,
        tags = emptyList(),
        moodAnalysis = null,
        takeaway = null,
        updatedAtMillis = now,
    )
}

fun currentDraftOrDefault(): JournalEntryDraft {
    return JournalEntryDraft(
        id = "",
        recordingSessionId = "",
        transcriptText = "",
        updatedAtMillis = System.currentTimeMillis(),
    )
}

fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return when {
        hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%02d:%02d".format(minutes, seconds)
    }
}

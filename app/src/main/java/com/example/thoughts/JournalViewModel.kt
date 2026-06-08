package com.example.thoughts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.thoughts.ui.events.UiAction
import com.example.thoughts.ui.events.UiEvent
import com.example.thoughts.ui.popup.PopupKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val uiEvents = _uiEvents.asSharedFlow()

    private val _currentDraft = MutableStateFlow<JournalEntryDraft?>(null)
    val currentDraft: StateFlow<JournalEntryDraft?> = _currentDraft.asStateFlow()

    private val _recordingSession = MutableStateFlow(RecordingSession(id = "", startedAtMillis = System.currentTimeMillis()))
    val recordingSession: StateFlow<RecordingSession> = _recordingSession.asStateFlow()

    private val _uploadState = MutableStateFlow(AudioUploadState.Local)
    val uploadState: StateFlow<AudioUploadState> = _uploadState.asStateFlow()

    private val _uploadError = MutableStateFlow<String?>(null)
    val uploadError: StateFlow<String?> = _uploadError.asStateFlow()

    private val _backendResult = MutableStateFlow<IngestionResponse?>(null)
    val backendResult: StateFlow<IngestionResponse?> = _backendResult.asStateFlow()

    private val _archivedEntries = MutableStateFlow<List<ArchiveEntrySummary>>(emptyList())
    val archivedEntries: StateFlow<List<ArchiveEntrySummary>> = _archivedEntries.asStateFlow()

    private val _selectedEntry = MutableStateFlow<JournalEntry?>(null)
    val selectedEntry: StateFlow<JournalEntry?> = _selectedEntry.asStateFlow()

    private val _userProfile = MutableStateFlow<ProfileResponse?>(null)
    val userProfile: StateFlow<ProfileResponse?> = _userProfile.asStateFlow()

    private val _dashboard = MutableStateFlow<DashboardResponse?>(null)
    val dashboard: StateFlow<DashboardResponse?> = _dashboard.asStateFlow()

    init {
        // Dashboard
        viewModelScope.launch {
            JournalRepository.getDashboardFlow().collect {
                _dashboard.value = it
            }
        }
        // Drafts
        viewModelScope.launch {
            val latest = JournalRepository.getLatestDraft()
            if (latest != null) {
                _currentDraft.value = latest
            }
        }
        // Profile
        viewModelScope.launch {
            JournalRepository.getProfileFlow().collect { profile ->
                if (profile != null) {
                    _userProfile.value = profile
                }
            }
        }
        // Transcription State Collection
        viewModelScope.launch {
            transcriptionEngine.state.collect { state ->
                _liveTranscriptText.value = state.transcript
                _liveTranscriptError.value = state.error
                _isLiveTranscribing.value = state.isTranscribing
                
                // Persist transcript to draft if it changes
                if (state.transcript.isNotEmpty()) {
                    updateTranscriptText(state.transcript)
                }
            }
        }
        // Model Status & AI Engine Initialization
        viewModelScope.launch {
            modelStatus.collect { status ->
                when (status) {
                    is ModelStatus.Downloaded -> {
                        analysisEngine.initialize().onFailure { error ->
                            Log.e(TAG, "AI Engine initialization failed", error)
                        }
                    }
                    is ModelStatus.Downloading -> {
                        pollModelDownloadStatus()
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun pollModelDownloadStatus() {
        viewModelScope.launch {
            while (modelStatus.value is ModelStatus.Downloading) {
                modelManager.updateProgress()
                delay(1000)
            }
        }
    }

    fun downloadModel() {
        modelManager.startDownload()
    }

    private val _userPreferences = MutableStateFlow<PreferencesResponse?>(null)
    val userPreferences: StateFlow<PreferencesResponse?> = _userPreferences.asStateFlow()

    private var timerJob: Job? = null
    private var audioFile: File? = null
    private var audioRecorder: AudioRecorder? = null
    private var uploadRetryCount = 0

    // --- On-Device Transcription Engine (Phase 2) ---
    private val transcriptionEngine: TranscriptionEngine = SystemTranscriptionEngine(application)

    // --- Model Management ---
    private val modelManager = ModelManager(application)
    val modelStatus = modelManager.status

    // --- On-Device AI Analysis Engine (Phase 3) ---
    private val analysisEngine: AnalysisEngine = MediaPipeAnalysisEngine(
        application,
        modelManager.getModelFile().absolutePath
    )

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _liveTranscriptText = MutableStateFlow("")
    val liveTranscriptText: StateFlow<String> = _liveTranscriptText.asStateFlow()

    private val _liveTranscriptError = MutableStateFlow<String?>(null)
    val liveTranscriptError: StateFlow<String?> = _liveTranscriptError.asStateFlow()

    private val _isLiveTranscribing = MutableStateFlow(false)
    val isLiveTranscribing: StateFlow<Boolean> = _isLiveTranscribing.asStateFlow()

    fun saveDraft(draft: JournalEntryDraft) {
        _currentDraft.value = draft
        viewModelScope.launch {
            JournalRepository.saveDraft(draft)
        }
    }

    fun ensureDraftInitialized() {
        if (_currentDraft.value == null) {
            val now = System.currentTimeMillis()
            val draft = createDefaultDraft(now)
            saveDraft(draft)
        }
    }

    private fun currentDraftOrDefault(): JournalEntryDraft {
        return _currentDraft.value ?: createDefaultDraft(System.currentTimeMillis())
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

        _liveTranscriptText.value = ""
        _liveTranscriptError.value = null
        _isLiveTranscribing.value = false

        // Start On-Device Transcription Engine (Phase 2)
        transcriptionEngine.start()

        // Create audio file and start recording
        audioFile = AudioFileManager.createAudioFile(getApplication())
        audioRecorder = AudioRecorder(
            context = getApplication(),
            onPcmChunk = { _, _ ->
                // Native system transcription (SystemTranscriptionEngine) doesn't require PCM chunks
                // but other implementations (like Whisper) might.
            },
        ).apply {
            start(audioFile!!)
        }
        Log.d(TAG, "Started recording to: ${audioFile?.absolutePath}")

        startTimer()
    }

    // Backend transcription methods removed for local-first migration
    // startBackendLiveTranscription(), stopBackendLiveTranscription(), handleBackendLiveTranscriptionMessage() ...

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
                transcriptionEngine.stop()
                current.copy(status = status)
            }
            RecordingStatus.Recording -> {
                audioRecorder?.resume()
                transcriptionEngine.start()
                current.copy(status = status, endedAtMillis = null)
            }
            RecordingStatus.Finished -> {
                audioRecorder?.stop()
                transcriptionEngine.stop()
                stopTimer()
                current.copy(
                    status = status,
                    endedAtMillis = now,
                    durationMs = now - current.startedAtMillis,
                )
            }
            RecordingStatus.Discarded -> {
                audioRecorder?.stop()
                transcriptionEngine.cancel()
                stopTimer()
                current.copy(status = status, endedAtMillis = now)
            }
            RecordingStatus.Error -> {
                audioRecorder?.stop()
                transcriptionEngine.stop()
                current.copy(status = status)
            }
            RecordingStatus.Idle -> {
                audioRecorder?.stop()
                transcriptionEngine.cancel()
                current.copy(status = status)
            }
        }
        _recordingSession.value = updatedSession
        persistRecordingSession(updatedSession)
    }

    fun finishRecordingAndUpload() {
        audioRecorder?.stop()
        transcriptionEngine.stop()
        stopTimer()

        val now = System.currentTimeMillis()
        val session = _recordingSession.value
        val updated = session.copy(
            status = RecordingStatus.Finished,
            endedAtMillis = now,
            durationMs = now - session.startedAtMillis,
        )
        _recordingSession.value = updated
        persistRecordingSession(updated)

        // Save AudioAsset metadata for tracking
        val assetMimeType = when (audioFile?.extension?.lowercase()) {
            "wav" -> "audio/wav"
            "m4a" -> "audio/m4a"
            else -> "application/octet-stream"
        }
        val asset = AudioAsset(
            id = "asset-${updated.id}",
            recordingSessionId = updated.id,
            localPath = audioFile?.absolutePath,
            mimeType = assetMimeType,
            durationMs = updated.durationMs,
            uploadState = AudioUploadState.Local
        )
        viewModelScope.launch {
            JournalRepository.saveAudioAsset(asset)
            val currentDraft = currentDraftOrDefault()
            val finalDraft = currentDraft.copy(audioAsset = asset)
            saveDraft(finalDraft)

            // Trigger On-Device Analysis (Phase 3)
            processJournalEntryLocally(finalDraft)
        }

        _uiEvents.tryEmit(
            UiEvent.Toast(
                message = "Recording saved locally",
                kind = PopupKind.Success,
            )
        )
    }

    private fun processJournalEntryLocally(draft: JournalEntryDraft) {
        if (draft.transcriptText.isBlank()) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            _uploadState.value = AudioUploadState.Processing
            
            analysisEngine.analyze(draft.transcriptText)
                .onSuccess { result ->
                    val updatedDraft = draft.copy(
                        tags = result.tags.map { JournalTag(it) },
                        moodAnalysis = MoodAnalysis(
                            label = result.mood,
                            score = result.moodScore.toFloat(),
                            explanation = result.moodExplanation
                        ),
                        takeaway = result.takeaway
                    )
                    saveDraft(updatedDraft)
                    _uploadState.value = AudioUploadState.Uploaded
                    _uiEvents.tryEmit(UiEvent.Toast("Insights generated", PopupKind.Success))
                }
                .onFailure { error ->
                    Log.e(TAG, "Local analysis failed", error)
                    _uploadState.value = AudioUploadState.Failed
                    _uiEvents.tryEmit(UiEvent.Toast("Analysis failed: ${error.message}", PopupKind.Error))
                }
                .also {
                    _isAnalyzing.value = false
                }
        }
    }

    fun discardRecording() {
        audioRecorder?.stop()
        transcriptionEngine.cancel()
        stopTimer()

        // Delete audio file
        audioFile?.let { AudioFileManager.deleteAudioFile(it) }
        audioFile = null

        // Reset state
        _recordingSession.value = RecordingSession(
            id = "",
            startedAtMillis = System.currentTimeMillis(),
        )

        _liveTranscriptText.value = ""
        _liveTranscriptError.value = null
        _isLiveTranscribing.value = false
    }

    // Local analysis will be implemented in Phase 3
    // private fun uploadAudioToBackend() { ... }

    fun retryUpload() {
        // No-op in local-first mode
    }

    fun loadArchivedEntries() {
        viewModelScope.launch {
            JournalRepository.refreshEntries()
            JournalRepository.getEntries().collect { entries ->
                _archivedEntries.value = entries.map { it.toArchiveEntrySummary() }
            }
        }
    }

    fun listArchivedEntries(): List<ArchiveEntrySummary> {
        return _archivedEntries.value
    }

    fun loadEntry(id: String) {
        viewModelScope.launch {
            val result = JournalRepository.getEntry(id)
            result.onSuccess { entry ->
                _selectedEntry.value = entry
            }
            result.onFailure { error ->
                Log.e(TAG, "Failed to load entry: $id", error)
                _selectedEntry.value = null
            }
        }
    }

    fun loadProfile() {
        viewModelScope.launch {
            JournalRepository.refreshProfile()
        }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            JournalRepository.refreshDashboard()
        }
    }

    fun loadPreferences() {
        viewModelScope.launch {
            JournalRepository.refreshPreferences()
            JournalRepository.getPreferencesFlow().collect { _userPreferences.value = it }
        }
    }

    // Local analysis will be implemented in Phase 3
    // private fun processingComplete(response: IngestionResponse) { ... }

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
        val currentId = _currentDraft.value?.id
        _currentDraft.value = null
        if (currentId != null) {
            viewModelScope.launch {
                JournalRepository.deleteDraft(currentId)
            }
        }

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

    private fun readRecordingSessionFromState(): RecordingSession {
        val id = savedStateHandle.get<String>(RecordingIdKey) ?: ""
        val startedAtMillis = savedStateHandle.get<Long>(RecordingStartedAtKey) ?: System.currentTimeMillis()
        val endedAtMillis = savedStateHandle.get<Long>(RecordingEndedAtKey)
        val durationMs = savedStateHandle.get<Long>(RecordingDurationKey) ?: 0L
        val status = savedStateHandle.get<String>(RecordingStatusKey)
            ?.let { RecordingStatus.fromString(it) }
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
        transcriptionEngine.release()
        analysisEngine.release()
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

fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = millis / (1000 * 60 * 60)
    return when {
        hours > 0 -> "%02d:%02d:%02d".format(hours, minutes, seconds)
        else -> "%02d:%02d".format(minutes, seconds)
    }
}

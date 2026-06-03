package com.example.thoughts

import android.content.Context
import android.util.Log
import com.example.thoughts.data.local.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "JournalRepository"

object JournalRepository {
    private lateinit var database: ThoughtsDatabase
    private lateinit var prefsManager: UserPreferencesManager
    private var initialized = false
    private val json = Json { ignoreUnknownKeys = true }

    fun initialize(context: Context) {
        if (initialized) return
        database = ThoughtsDatabase.getDatabase(context)
        prefsManager = UserPreferencesManager(context)
        initialized = true
    }

    fun ensureInitialized(context: Context) {
        initialize(context.applicationContext)
    }

    private val dao get() = database.journalDao()

    // --- Dashboard ---

    fun getDashboardFlow(): Flow<DashboardResponse?> {
        return dao.getDashboardCache("current_dashboard").map { entity ->
            entity?.let {
                DashboardResponse(
                    prompt = it.prompt,
                    prompt_status = it.promptStatus,
                    streak_count = it.streakCount,
                    entry_count = it.entryCount
                )
            }
        }
    }

    suspend fun refreshDashboard() {
        BackendService.getDashboard()
            .onSuccess { response ->
                dao.saveDashboardCache(
                    DashboardCacheEntity(
                        prompt = response.prompt,
                        promptStatus = response.prompt_status,
                        streakCount = response.streak_count,
                        entryCount = response.entry_count,
                        updatedAtMillis = System.currentTimeMillis()
                    )
                )
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to load dashboard", error)
            }
    }

    // --- Journal Entries ---

    fun getEntries(): Flow<List<JournalEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshEntries() {
        BackendService.listJournalEntries(limit = 50)
            .onSuccess { response ->
                response.entries.forEach { summary ->
                    dao.insertEntry(summary.toEntity())
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to load journal entries", error)
            }
    }

    suspend fun getEntry(id: String, forceRefresh: Boolean = false): Result<JournalEntry> {
        if (!forceRefresh) {
            val localEntity = dao.getEntryById(id)
            if (localEntity != null) {
                val localTranscript = dao.getTranscriptById(localEntity.transcriptId)
                // Only return local if we have both transcript and audio URL (if it was an audio entry)
                if (localTranscript != null && localTranscript.fullText.isNotBlank() && localEntity.audioRemoteUrl != null) {
                    return Result.success(localEntity.toDomain(localTranscript))
                }
            }
        }

        return BackendService.getJournalEntry(id).map { response ->
            val entry = response.toJournalEntry()
            dao.insertTranscript(
                TranscriptEntity(
                    id = entry.transcript.id,
                    recordingSessionId = entry.recordingSessionId,
                    fullText = entry.transcript.fullText,
                    languageTag = entry.transcript.languageTag,
                    confidence = entry.transcript.confidence,
                )
            )
            dao.insertEntry(entry.toEntity())
            entry
        }
    }

    // --- Drafts & Uploads ---

    suspend fun getLatestDraft(): JournalEntryDraft? {
        val entity = dao.getLatestDraft() ?: return null
        val audioAsset = entity.audioAssetId?.let { dao.getAudioAssetById(it) }
        return entity.toDomain(audioAsset)
    }

    suspend fun saveDraft(draft: JournalEntryDraft) {
        draft.audioAsset?.let { saveAudioAsset(it) }
        dao.insertDraft(draft.toEntity())
    }

    suspend fun getDraft(id: String): JournalEntryDraft? {
        val entity = dao.getDraftById(id) ?: return null
        val audioAsset = entity.audioAssetId?.let { dao.getAudioAssetById(it) }
        return entity.toDomain(audioAsset)
    }

    suspend fun deleteDraft(id: String) {
        dao.deleteDraft(id)
    }

    suspend fun getAssetsToUpload(): List<AudioAssetEntity> {
        return dao.getAssetsToUpload()
    }

    fun getAudioAssetFlow(id: String): Flow<AudioAsset?> {
        return dao.getAudioAssetFlow(id).map { it?.toDomain() }
    }

    suspend fun saveAudioAsset(asset: AudioAsset) {
        dao.insertAudioAsset(asset.toEntity())
    }

    suspend fun updateAudioUploadState(assetId: String, state: AudioUploadState) {
        dao.updateAudioAssetState(assetId, state.name)
    }

    suspend fun persistUploadResult(
        asset: AudioAssetEntity,
        response: IngestionResponse,
        languageTag: String,
    ) {
        val remoteUrl = response.audioRemoteUrl ?: asset.remoteUrl
        dao.completeAudioAssetUpload(asset.id, AudioUploadState.Uploaded.name, remoteUrl)

        val transcriptId = "transcript-${response.entryId ?: asset.recordingSessionId}"
        dao.insertTranscript(
            TranscriptEntity(
                id = transcriptId,
                recordingSessionId = asset.recordingSessionId,
                fullText = response.transcript,
                languageTag = languageTag,
                confidence = response.moodConfidence,
            )
        )

        val existingDraft = dao.getDraftByRecordingSessionId(asset.recordingSessionId)
        val updatedAssetEntity = asset.copy(
            remoteUrl = remoteUrl,
            uploadState = AudioUploadState.Uploaded.name,
        )
        val updatedAsset = updatedAssetEntity.toDomain()
        val mood = response.moodLabel?.let { label ->
            MoodAnalysis(
                label = label,
                score = response.moodScore ?: 0f,
                confidence = response.moodConfidence,
                explanation = response.moodExplanation,
            )
        }
        val draft = existingDraft?.toDomain(updatedAssetEntity)?.copy(
            transcriptText = response.transcript,
            tags = response.tags.map { JournalTag(it, TagSource.Generated) },
            moodAnalysis = mood,
            audioAsset = updatedAsset,
            updatedAtMillis = System.currentTimeMillis(),
        ) ?: JournalEntryDraft(
            id = response.draftId ?: "draft-${asset.recordingSessionId}",
            recordingSessionId = asset.recordingSessionId,
            transcriptText = response.transcript,
            audioAsset = updatedAsset,
            tags = response.tags.map { JournalTag(it, TagSource.Generated) },
            moodAnalysis = mood,
            updatedAtMillis = System.currentTimeMillis(),
        )
        dao.insertDraft(draft.toEntity())
    }

    suspend fun savePreferences(preferences: PreferencesResponse) {
        prefsManager.saveAppPreferences(json.encodeToString(PreferencesResponse.serializer(), preferences))
    }

    // --- Profile & Preferences ---

    fun getProfileFlow(): Flow<ProfileResponse?> {
        return prefsManager.userProfileFlow.map { jsonString ->
            jsonString?.let { json.decodeFromString<ProfileResponse>(it) }
        }
    }

    suspend fun refreshProfile() {
        BackendService.getProfile().onSuccess { profile ->
            prefsManager.saveUserProfile(json.encodeToString(ProfileResponse.serializer(), profile))
        }
    }

    fun getPreferencesFlow(): Flow<PreferencesResponse?> {
        return prefsManager.appPreferencesFlow.map { jsonString ->
            jsonString?.let { json.decodeFromString<PreferencesResponse>(it) }
        }
    }

    suspend fun refreshPreferences() {
        BackendService.getPreferences().onSuccess { prefs ->
            prefsManager.saveAppPreferences(json.encodeToString(PreferencesResponse.serializer(), prefs))
        }
    }

    suspend fun savePreferences(prefs: PreferencesResponse) {
        // Prepare patch request
        val request = UpdatePreferencesRequest(
            appearance_mode = prefs.theme,
            notifications_enabled = prefs.notifications_enabled,
            prompt_reminder_time = prefs.reminder_time,
            language = prefs.language
        )

        // Sync to backend first
        BackendService.updatePreferences(request).onSuccess { updated ->
            prefsManager.saveAppPreferences(json.encodeToString(PreferencesResponse.serializer(), updated))
        }.onFailure {
            // Even if backend fails, we save locally for offline-first feel
            // but log the error
            Log.e(TAG, "Failed to sync preferences to backend", it)
            prefsManager.saveAppPreferences(json.encodeToString(PreferencesResponse.serializer(), prefs))
        }
    }
}

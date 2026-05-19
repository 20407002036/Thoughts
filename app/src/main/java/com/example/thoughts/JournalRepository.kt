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
    private val json = Json { ignoreUnknownKeys = true }

    fun initialize(context: Context) {
        database = ThoughtsDatabase.getDatabase(context)
        prefsManager = UserPreferencesManager(context)
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
        BackendService.listJournalEntries().onSuccess { response ->
            response.entries.forEach { summary ->
                dao.insertEntry(summary.toEntity())
            }
        }
    }

    suspend fun getEntry(id: String): Result<JournalEntry> {
        val local = dao.getEntryById(id)?.toDomain()
        if (local != null) return Result.success(local)

        return BackendService.getJournalEntry(id).map { response ->
            val entry = response.toJournalEntry()
            dao.insertEntry(entry.toEntity())
            entry
        }
    }

    // --- Drafts & Uploads ---

    suspend fun getLatestDraft(): JournalEntryDraft? {
        return dao.getLatestDraft()?.toDomain()
    }

    suspend fun saveDraft(draft: JournalEntryDraft) {
        dao.insertDraft(draft.toEntity())
    }

    suspend fun getDraft(id: String): JournalEntryDraft? {
        return dao.getDraftById(id)?.toDomain()
    }

    suspend fun deleteDraft(id: String) {
        dao.deleteDraft(id)
    }

    suspend fun saveAudioAsset(asset: AudioAsset) {
        dao.insertAudioAsset(asset.toEntity())
    }

    suspend fun updateAudioUploadState(assetId: String, state: AudioUploadState) {
        dao.updateAudioAssetState(assetId, state.name)
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
}

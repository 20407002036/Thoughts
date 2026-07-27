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

    private val currentUserId: String?
        get() = AuthSessionManager.session.value?.userId

    suspend fun clearAllUserData() {
        if (::database.isInitialized) {
            dao.clearAllData()
        }
        if (::prefsManager.isInitialized) {
            prefsManager.clearAll()
        }
    }

    // --- Dashboard ---

    fun getDashboardFlow(): Flow<DashboardResponse?> {
        val userId = currentUserId ?: return flowOf(null)
        return dao.getDashboardCache(userId).map { entity ->
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
        val userId = currentUserId ?: return
        BackendService.getDashboard()
            .onSuccess { response ->
                dao.saveDashboardCache(
                    DashboardCacheEntity(
                        userId = userId,
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
        val userId = currentUserId ?: return flowOf(emptyList())
        return dao.getAllEntries(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshEntries() {
        val userId = currentUserId ?: return
        BackendService.listJournalEntries(limit = 50)
            .onSuccess { response ->
                response.entries.forEach { summary ->
                    dao.insertEntry(summary.toEntity(userId))
                }
            }
            .onFailure { error ->
                Log.e(TAG, "Failed to load journal entries", error)
            }
    }

    suspend fun getEntry(id: String): Result<JournalEntry> {
        val userId = currentUserId ?: return Result.failure(IllegalStateException("No active user session"))
        val local = dao.getEntryById(id, userId)?.toDomain()
        if (local != null) return Result.success(local)

        return BackendService.getJournalEntry(id).map { response ->
            val entry = response.toJournalEntry()
            dao.insertEntry(entry.toEntity(userId))
            entry
        }
    }

    // --- Drafts & Uploads ---

    suspend fun getLatestDraft(): JournalEntryDraft? {
        val userId = currentUserId ?: return null
        return dao.getLatestDraft(userId)?.toDomain()
    }

    suspend fun saveDraft(draft: JournalEntryDraft) {
        val userId = currentUserId ?: return
        dao.insertDraft(draft.toEntity(userId))
    }

    suspend fun getDraft(id: String): JournalEntryDraft? {
        val userId = currentUserId ?: return null
        return dao.getDraftById(id, userId)?.toDomain()
    }

    suspend fun deleteDraft(id: String) {
        val userId = currentUserId ?: return
        dao.deleteDraft(id, userId)
    }

    suspend fun saveAudioAsset(asset: AudioAsset) {
        val userId = currentUserId ?: return
        dao.insertAudioAsset(asset.toEntity(userId))
    }

    suspend fun updateAudioUploadState(assetId: String, state: AudioUploadState) {
        val userId = currentUserId ?: return
        dao.updateAudioAssetState(assetId, userId, state.name)
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

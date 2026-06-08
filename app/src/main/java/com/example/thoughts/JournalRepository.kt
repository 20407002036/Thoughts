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
        val entries = dao.getAllEntries().first()
        val entryCount = entries.size
        val streak = calculateStreak(entries)

        dao.saveDashboardCache(
            DashboardCacheEntity(
                prompt = "How was your day?", // Default local prompt
                promptStatus = "active",
                streakCount = streak,
                entryCount = entryCount,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    private fun calculateStreak(entries: List<JournalEntryEntity>): Int {
        if (entries.isEmpty()) return 0

        val dates = entries.map { it.createdAtMillis }
            .map { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sortedDescending()

        var streak = 0
        var currentDate = java.time.LocalDate.now()

        val hasEntryToday = dates.contains(currentDate)
        val hasEntryYesterday = dates.contains(currentDate.minusDays(1))

        if (!hasEntryToday && !hasEntryYesterday) return 0

        var checkDate = if (hasEntryToday) currentDate else currentDate.minusDays(1)

        for (date in dates) {
            if (date == checkDate) {
                streak++
                checkDate = checkDate.minusDays(1)
            } else if (date.isBefore(checkDate)) {
                break
            }
        }
        return streak
    }

    // --- Journal Entries ---

    fun getEntries(): Flow<List<JournalEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun refreshEntries() {
        // No-op in local-first mode: Room is the source of truth
    }

    suspend fun getEntry(id: String): Result<JournalEntry> {
        val local = dao.getEntryById(id)?.toDomain()
        return if (local != null) {
            Result.success(local)
        } else {
            Result.failure(Exception("Entry not found locally"))
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
        // No-op in local-first mode
    }

    fun getPreferencesFlow(): Flow<PreferencesResponse?> {
        return prefsManager.appPreferencesFlow.map { jsonString ->
            jsonString?.let { json.decodeFromString<PreferencesResponse>(it) }
        }
    }

    suspend fun refreshPreferences() {
        // No-op in local-first mode
    }

    suspend fun savePreferences(prefs: PreferencesResponse) {
        prefsManager.saveAppPreferences(json.encodeToString(PreferencesResponse.serializer(), prefs))
    }
}

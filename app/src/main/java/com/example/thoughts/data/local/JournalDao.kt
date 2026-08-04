package com.example.thoughts.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun getAllEntries(userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id AND userId = :userId")
    suspend fun getEntryById(id: String, userId: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id AND userId = :userId")
    suspend fun deleteEntry(id: String, userId: String)

    @Query("SELECT * FROM journal_drafts WHERE userId = :userId ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getLatestDraft(userId: String): JournalDraftEntity?

    @Query("SELECT * FROM journal_drafts WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun getAllDrafts(userId: String): Flow<List<JournalDraftEntity>>

    @Query("SELECT * FROM journal_drafts WHERE id = :id AND userId = :userId")
    suspend fun getDraftById(id: String, userId: String): JournalDraftEntity?

    @Query("SELECT * FROM journal_drafts WHERE recordingSessionId = :recordingSessionId AND userId = :userId ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getDraftByRecordingSessionId(recordingSessionId: String, userId: String): JournalDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: JournalDraftEntity)

    @Query("DELETE FROM journal_drafts WHERE id = :id AND userId = :userId")
    suspend fun deleteDraft(id: String, userId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioAsset(asset: AudioAssetEntity)

    @Query("SELECT * FROM audio_assets WHERE id = :id AND userId = :userId")
    suspend fun getAudioAssetById(id: String, userId: String): AudioAssetEntity?

    @Query("UPDATE audio_assets SET uploadState = :state WHERE id = :id AND userId = :userId")
    suspend fun updateAudioAssetState(id: String, userId: String, state: String)
    
    @Query("SELECT * FROM audio_assets WHERE id = :id AND userId = :userId")
    fun getAudioAssetFlow(id: String, userId: String): Flow<AudioAssetEntity?>

    @Query("SELECT * FROM audio_assets WHERE userId = :userId AND (uploadState = 'Local' OR uploadState = 'Failed' OR uploadState = 'Uploading')")
    suspend fun getAssetsToUpload(userId: String): List<AudioAssetEntity>

    @Query("UPDATE audio_assets SET uploadState = :state, remoteUrl = :remoteUrl WHERE id = :id AND userId = :userId")
    suspend fun completeAudioAssetUpload(id: String, userId: String, state: String, remoteUrl: String?)

    @Query("SELECT * FROM audio_assets WHERE userId = :userId")
    suspend fun getAudioAssetsByUser(userId: String): List<AudioAssetEntity>

    @Query("SELECT * FROM audio_assets WHERE userId != :userId")
    suspend fun getAudioAssetsNotOwnedBy(userId: String): List<AudioAssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity)

    @Transaction
    suspend fun saveEntryWithTranscript(entry: JournalEntryEntity, transcript: TranscriptEntity) {
        insertEntry(entry)
        insertTranscript(transcript)
    }

    @Query("SELECT * FROM transcripts WHERE id = :id AND userId = :userId")
    suspend fun getTranscriptById(id: String, userId: String): TranscriptEntity?

    @Query("SELECT * FROM dashboard_cache WHERE userId = :userId")
    fun getDashboardCache(userId: String): Flow<DashboardCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDashboardCache(cache: DashboardCacheEntity)

    @Query("DELETE FROM journal_entries WHERE userId = :userId")
    suspend fun deleteEntriesForUser(userId: String)

    @Query("DELETE FROM journal_drafts WHERE userId = :userId")
    suspend fun deleteDraftsForUser(userId: String)

    @Query("DELETE FROM audio_assets WHERE userId = :userId")
    suspend fun deleteAudioAssetsForUser(userId: String)

    @Query("DELETE FROM transcripts WHERE userId = :userId")
    suspend fun deleteTranscriptsForUser(userId: String)

    @Query("DELETE FROM dashboard_cache WHERE userId = :userId")
    suspend fun deleteDashboardCacheForUser(userId: String)

    @Transaction
    suspend fun deleteUserData(userId: String) {
        deleteEntriesForUser(userId)
        deleteDraftsForUser(userId)
        deleteAudioAssetsForUser(userId)
        deleteTranscriptsForUser(userId)
        deleteDashboardCacheForUser(userId)
    }

    @Query("DELETE FROM journal_entries WHERE userId != :userId")
    suspend fun deleteEntriesNotOwnedBy(userId: String)

    @Query("DELETE FROM journal_drafts WHERE userId != :userId")
    suspend fun deleteDraftsNotOwnedBy(userId: String)

    @Query("DELETE FROM audio_assets WHERE userId != :userId")
    suspend fun deleteAudioAssetsNotOwnedBy(userId: String)

    @Query("DELETE FROM transcripts WHERE userId != :userId")
    suspend fun deleteTranscriptsNotOwnedBy(userId: String)

    @Query("DELETE FROM dashboard_cache WHERE userId != :userId")
    suspend fun deleteDashboardCacheNotOwnedBy(userId: String)

    @Transaction
    suspend fun deleteOtherUsersData(userId: String) {
        deleteEntriesNotOwnedBy(userId)
        deleteDraftsNotOwnedBy(userId)
        deleteAudioAssetsNotOwnedBy(userId)
        deleteTranscriptsNotOwnedBy(userId)
        deleteDashboardCacheNotOwnedBy(userId)
    }
}

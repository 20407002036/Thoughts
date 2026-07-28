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

    @Query("SELECT * FROM journal_drafts WHERE recordingSessionId = :recordingSessionId ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getDraftByRecordingSessionId(recordingSessionId: String): JournalDraftEntity?

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
    
    @Query("SELECT * FROM audio_assets WHERE id = :id")
    fun getAudioAssetFlow(id: String): Flow<AudioAssetEntity?>

    @Query("SELECT * FROM audio_assets WHERE uploadState = 'Local' OR uploadState = 'Failed' OR uploadState = 'Uploading'")
    suspend fun getAssetsToUpload(): List<AudioAssetEntity>

    @Query("UPDATE audio_assets SET uploadState = :state, remoteUrl = :remoteUrl WHERE id = :id")
    suspend fun completeAudioAssetUpload(id: String, state: String, remoteUrl: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity)

    @Query("SELECT * FROM transcripts WHERE id = :id AND userId = :userId")
    suspend fun getTranscriptById(id: String, userId: String): TranscriptEntity?

    @Query("SELECT * FROM dashboard_cache WHERE userId = :userId")
    fun getDashboardCache(userId: String): Flow<DashboardCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDashboardCache(cache: DashboardCacheEntity)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllEntries()

    @Query("DELETE FROM journal_drafts")
    suspend fun deleteAllDrafts()

    @Query("DELETE FROM audio_assets")
    suspend fun deleteAllAudioAssets()

    @Query("DELETE FROM transcripts")
    suspend fun deleteAllTranscripts()

    @Query("DELETE FROM dashboard_cache")
    suspend fun deleteAllDashboardCache()

    @Transaction
    suspend fun clearAllData() {
        deleteAllEntries()
        deleteAllDrafts()
        deleteAllAudioAssets()
        deleteAllTranscripts()
        deleteAllDashboardCache()
    }
}

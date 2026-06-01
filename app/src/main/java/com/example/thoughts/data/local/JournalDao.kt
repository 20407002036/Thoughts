package com.example.thoughts.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY createdAtMillis DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: String)

    @Query("SELECT * FROM journal_drafts ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getLatestDraft(): JournalDraftEntity?

    @Query("SELECT * FROM journal_drafts ORDER BY updatedAtMillis DESC")
    fun getAllDrafts(): Flow<List<JournalDraftEntity>>

    @Query("SELECT * FROM journal_drafts WHERE id = :id")
    suspend fun getDraftById(id: String): JournalDraftEntity?

    @Query("SELECT * FROM journal_drafts WHERE recordingSessionId = :recordingSessionId ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun getDraftByRecordingSessionId(recordingSessionId: String): JournalDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: JournalDraftEntity)

    @Query("DELETE FROM journal_drafts WHERE id = :id")
    suspend fun deleteDraft(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioAsset(asset: AudioAssetEntity)

    @Query("SELECT * FROM audio_assets WHERE id = :id")
    suspend fun getAudioAssetById(id: String): AudioAssetEntity?

    @Query("SELECT * FROM audio_assets WHERE id = :id")
    fun getAudioAssetFlow(id: String): Flow<AudioAssetEntity?>

    @Query("SELECT * FROM audio_assets WHERE uploadState = 'Local' OR uploadState = 'Failed' OR uploadState = 'Uploading'")
    suspend fun getAssetsToUpload(): List<AudioAssetEntity>

    @Query("UPDATE audio_assets SET uploadState = :state WHERE id = :id")
    suspend fun updateAudioAssetState(id: String, state: String)

    @Query("UPDATE audio_assets SET uploadState = :state, remoteUrl = :remoteUrl WHERE id = :id")
    suspend fun completeAudioAssetUpload(id: String, state: String, remoteUrl: String?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity)

    @Query("SELECT * FROM transcripts WHERE id = :id")
    suspend fun getTranscriptById(id: String): TranscriptEntity?

    @Query("SELECT * FROM dashboard_cache WHERE id = :id")
    fun getDashboardCache(id: String): Flow<DashboardCacheEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDashboardCache(cache: DashboardCacheEntity)
}

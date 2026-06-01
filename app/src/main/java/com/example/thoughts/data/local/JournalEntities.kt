package com.example.thoughts.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val recordingSessionId: String,
    val title: String?,
    val createdAtMillis: Long,
    val recordedAtMillis: Long,
    val transcriptId: String,
    val takeaway: String?,
    val status: String, // Using String for enum
    val moodLabel: String?,
    val moodScore: Float,
    val audioAssetId: String? = null,
    val audioRemoteUrl: String? = null,
)

@Entity(tableName = "journal_drafts")
data class JournalDraftEntity(
    @PrimaryKey val id: String,
    val recordingSessionId: String,
    val title: String?,
    val transcriptText: String,
    val audioAssetId: String?,
    val takeaway: String?,
    val updatedAtMillis: Long,
)

@Entity(tableName = "audio_assets")
data class AudioAssetEntity(
    @PrimaryKey val id: String,
    val recordingSessionId: String,
    val localPath: String?,
    val remoteUrl: String?,
    val mimeType: String,
    val durationMs: Long,
    val sizeBytes: Long?,
    val uploadState: String, // Using String for enum
)

@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey val id: String,
    val recordingSessionId: String,
    val fullText: String,
    val languageTag: String,
    val confidence: Float?,
)

@Entity(tableName = "dashboard_cache")
data class DashboardCacheEntity(
    @PrimaryKey val id: String = "current_dashboard",
    val prompt: String?,
    val promptStatus: String,
    val streakCount: Int,
    val entryCount: Int,
    val updatedAtMillis: Long
)

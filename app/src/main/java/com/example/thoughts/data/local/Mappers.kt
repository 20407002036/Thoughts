package com.example.thoughts.data.local

import com.example.thoughts.*

fun JournalEntrySummaryResponse.toEntity(userId: String): JournalEntryEntity {
    val createdAtMillis = createdAt?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis()
    return JournalEntryEntity(
        id = entryId ?: id,
        userId = userId,
        recordingSessionId = "unknown", // Summary doesn't provide this
        title = title,
        createdAtMillis = createdAtMillis,
        recordedAtMillis = createdAtMillis,
        transcriptId = "transcript-${entryId ?: id}",
        takeaway = summary,
        status = status ?: "Ready",
        moodLabel = moodLabel,
        moodScore = 0f
    )
}

fun JournalEntryEntity.toDomain(): JournalEntry {
    // This is a simplified mapping. In a full app, we'd fetch the TranscriptEntity.
    // For now, we store the minimal required fields in the entity and use a dummy transcript
    // or fetch it from the DAO in the repository.
    return JournalEntry(
        id = id,
        recordingSessionId = recordingSessionId,
        title = title,
        createdAtMillis = createdAtMillis,
        recordedAtMillis = recordedAtMillis,
        transcript = Transcript(id = transcriptId, recordingSessionId = recordingSessionId, fullText = ""),
        takeaway = takeaway,
        status = JournalEntryStatus.fromString(status),
    )
}

fun JournalEntry.toEntity(userId: String): JournalEntryEntity {
    return JournalEntryEntity(
        id = id,
        userId = userId,
        recordingSessionId = recordingSessionId,
        title = title,
        createdAtMillis = createdAtMillis,
        recordedAtMillis = recordedAtMillis,
        transcriptId = transcript.id,
        takeaway = takeaway,
        status = status.name,
        moodLabel = moodAnalysis?.label,
        moodScore = moodAnalysis?.score ?: 0f
    )
}

fun JournalDraftEntity.toDomain(audioAssetEntity: AudioAssetEntity?): JournalEntryDraft {
    return JournalEntryDraft(
        id = id,
        recordingSessionId = recordingSessionId,
        title = title,
        transcriptText = transcriptText,
        audioAsset = audioAssetEntity?.toDomain(),
        takeaway = takeaway,
        updatedAtMillis = updatedAtMillis
    )
}

fun JournalDraftEntity.toDomain(): JournalEntryDraft {
    return toDomain(audioAssetEntity = null)
}

fun JournalEntryDraft.toEntity(userId: String): JournalDraftEntity {
    return JournalDraftEntity(
        id = id,
        userId = userId,
        recordingSessionId = recordingSessionId,
        title = title,
        transcriptText = transcriptText,
        audioAssetId = audioAsset?.id,
        takeaway = takeaway,
        updatedAtMillis = updatedAtMillis
    )
}

fun AudioAsset.toEntity(userId: String): AudioAssetEntity {
    return AudioAssetEntity(
        id = id,
        userId = userId,
        recordingSessionId = recordingSessionId,
        localPath = localPath,
        remoteUrl = remoteUrl,
        mimeType = mimeType,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        uploadState = uploadState.name
    )
}

fun AudioAssetEntity.toDomain(): AudioAsset {
    return AudioAsset(
        id = id,
        recordingSessionId = recordingSessionId,
        localPath = localPath,
        remoteUrl = remoteUrl,
        mimeType = mimeType,
        durationMs = durationMs,
        sizeBytes = sizeBytes,
        uploadState = AudioUploadState.fromString(uploadState)
    )
}

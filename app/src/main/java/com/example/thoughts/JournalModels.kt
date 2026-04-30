package com.example.thoughts

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
@Serializable
enum class RecordingStatus {
    Idle,
    Recording,
    Paused,
    Finished,
    Discarded,
    Error,
}

@Serializable
enum class AudioUploadState {
    Local,
    Uploading,
    Processing,
    Uploaded,
    Failed,
}

@Serializable
enum class JournalEntryStatus {
    Draft,
    Ready,
    Saved,
    Uploaded,
}

@Serializable
enum class TagSource {
    Generated,
    Manual,
}

@Serializable
data class RecordingSession(
    val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null,
    val durationMs: Long = 0L,
    val status: RecordingStatus = RecordingStatus.Idle,
)

@Serializable
data class TranscriptSegment(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val confidence: Float? = null,
)

@Serializable
data class Transcript(
    val id: String,
    val recordingSessionId: String,
    val fullText: String,
    val languageTag: String = "en-US",
    val confidence: Float? = null,
    val segments: List<TranscriptSegment> = emptyList(),
)

@Serializable
data class AudioAsset(
    val id: String,
    val recordingSessionId: String,
    val localPath: String? = null,
    val remoteUrl: String? = null,
    val mimeType: String = "audio/m4a",
    val durationMs: Long = 0L,
    val sizeBytes: Long? = null,
    val uploadState: AudioUploadState = AudioUploadState.Local,
) {
    init {
        require(localPath != null || remoteUrl != null) {
            "AudioAsset requires either a localPath or a remoteUrl."
        }
    }
}

@Serializable
data class JournalTag(
    val name: String,
    val source: TagSource = TagSource.Generated,
)

@Serializable
data class MoodAnalysis(
    val label: String,
    val score: Float,
    val confidence: Float? = null,
    val explanation: String? = null,
)

@Serializable
data class JournalEntryDraft(
    val id: String,
    val recordingSessionId: String,
    val title: String? = null,
    val transcriptText: String,
    val audioAsset: AudioAsset? = null,
    val tags: List<JournalTag> = emptyList(),
    val moodAnalysis: MoodAnalysis? = null,
    val takeaway: String? = null,
    val updatedAtMillis: Long,
)

@Serializable
data class JournalEntry(
    val id: String,
    val recordingSessionId: String,
    val title: String? = null,
    val createdAtMillis: Long,
    val recordedAtMillis: Long,
    val transcript: Transcript,
    val audioAsset: AudioAsset? = null,
    val tags: List<JournalTag> = emptyList(),
    val moodAnalysis: MoodAnalysis? = null,
    val takeaway: String? = null,
    val status: JournalEntryStatus = JournalEntryStatus.Draft,
)

@Serializable
data class EntryUploadRequest(
    val entryId: String,
    val title: String?,
    val createdAtMillis: Long,
    val recordedAtMillis: Long,
    val transcriptText: String,
    val audioRemoteUrl: String?,
    val tags: List<String>,
    val moodLabel: String?,
    val moodScore: Float?,
    val takeaway: String?,
)

@Serializable
data class EntryResponse(
    val entryId: String,
    val serverId: String,
    val savedAtMillis: Long,
    val title: String?,
    val transcriptText: String,
    val audioRemoteUrl: String?,
    val tags: List<String> = emptyList(),
)

@Serializable
data class IngestionAnalysis(
    @SerialName("mood")
    val mood: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("summary")
    val summary: String? = null,
    @SerialName("themes")
    val themes: List<String> = emptyList(),
    @SerialName("insights")
    val insights: List<String> = emptyList(),
)

@Serializable
data class IngestionResponse(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val transcript: String = "",
    val analysis: IngestionAnalysis? = null,
    @SerialName("audio_path")
    val audioPath: String? = null,
    @SerialName("audio_signed_url")
    val audioSignedUrl: String? = null,
    @SerialName("prompt_version")
    val promptVersion: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    // Compatibility accessors for existing UI/data flow code paths.
    val moodLabel: String?
        get() = analysis?.mood

    val moodScore: Float?
        get() = null

    val moodConfidence: Float?
        get() = null

    val moodExplanation: String?
        get() = analysis?.summary

    val tags: List<String>
        get() = analysis?.themes ?: emptyList()

    val audioRemoteUrl: String?
        get() = audioSignedUrl

    val processingDurationMs: Long?
        get() = null
}
fun JournalEntryDraft.toJournalEntry(
    transcriptId: String,
    createdAtMillis: Long,
    recordedAtMillis: Long,
) = JournalEntry(
    id = id,
    recordingSessionId = recordingSessionId,
    title = title,
    createdAtMillis = createdAtMillis,
    recordedAtMillis = recordedAtMillis,
    transcript = Transcript(
        id = transcriptId,
        recordingSessionId = recordingSessionId,
        fullText = transcriptText,
    ),
    audioAsset = audioAsset,
    tags = tags,
    moodAnalysis = moodAnalysis,
    takeaway = takeaway,
    status = JournalEntryStatus.Ready,
)

fun JournalEntry.toUploadRequest() = EntryUploadRequest(
    entryId = id,
    title = title,
    createdAtMillis = createdAtMillis,
    recordedAtMillis = recordedAtMillis,
    transcriptText = transcript.fullText,
    audioRemoteUrl = audioAsset?.remoteUrl,
    tags = tags.map { it.name },
    moodLabel = moodAnalysis?.label,
    moodScore = moodAnalysis?.score,
    takeaway = takeaway,
)

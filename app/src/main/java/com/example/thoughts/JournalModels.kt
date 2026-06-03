package com.example.thoughts

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

object EntryTranscriptResponseSerializer : KSerializer<EntryTranscriptResponse> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("EntryTranscriptResponse", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): EntryTranscriptResponse {
        val jsonDecoder = decoder as? JsonDecoder ?: error("EntryTranscriptResponseSerializer only works with JSON")
        val element = jsonDecoder.decodeJsonElement()

        return when (element) {
            is JsonPrimitive -> EntryTranscriptResponse(fullText = element.content)
            is JsonObject -> {
                val fullText = element["full_text"]?.jsonPrimitive?.content
                    ?: element["fullText"]?.jsonPrimitive?.content
                    ?: element["transcript"]?.jsonPrimitive?.content
                    ?: ""
                EntryTranscriptResponse(fullText = fullText)
            }
            else -> EntryTranscriptResponse(fullText = "")
        }
    }

    override fun serialize(encoder: Encoder, value: EntryTranscriptResponse) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("EntryTranscriptResponseSerializer only works with JSON")
        jsonEncoder.encodeJsonElement(JsonObject(mapOf("full_text" to JsonPrimitive(value.fullText))))
    }
}
@Serializable
enum class RecordingStatus {
    Idle,
    Recording,
    Paused,
    Finished,
    Discarded,
    Error;

    companion object {
        fun fromString(value: String): RecordingStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: Idle
        }
    }
}

@Serializable
enum class AudioUploadState {
    Local,
    Uploading,
    Processing,
    Uploaded,
    Failed;

    companion object {
        fun fromString(value: String): AudioUploadState {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: Local
        }
    }
}

@Serializable
enum class JournalEntryStatus {
    Draft,
    Ready,
    Saved,
    Uploaded,
    Completed;

    companion object {
        fun fromString(value: String): JournalEntryStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: Draft
        }
    }
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
    val mimeType: String = "audio/wav",
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
    @SerialName("recording_id")
    val recordingId: String? = null,
    val status: String? = null,
    @SerialName("progress_percent")
    val progressPercent: Int? = null,
    @SerialName("error_message")
    val errorMessage: String? = null,
    @SerialName("entry_id")
    val entryId: String? = null,
    @SerialName("draft_id")
    val draftId: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val transcript: String = "",
    val analysis: IngestionAnalysis? = null,
    @SerialName("audio_path")
    val audioPath: String? = null,
    @SerialName("audio_url")
    val audioUrl: String? = null,
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
        get() = audioSignedUrl ?: audioUrl

    val processingDurationMs: Long?
        get() = null
}

@Deprecated("Use IngestionResponse instead. Backend endpoint /v1/journals/ingest returns full entry data.")
@Serializable
data class RecordingUploadResponse(
    @SerialName("recording_id")
    val recordingId: String,
    val status: String,
    @SerialName("progress_percent")
    val progressPercent: Int = 0,
    @SerialName("error_message")
    val errorMessage: String? = null,
    @SerialName("entry_id")
    val entryId: String? = null,
    @SerialName("draft_id")
    val draftId: String? = null,
)

@Serializable
data class EntryTranscriptResponse(
    @SerialName("full_text")
    val fullText: String,
)

@Serializable
data class EntryTagResponse(
    val name: String? = null,
    @SerialName("label")
    val label: String? = null,
    val source: String? = null,
)

@Serializable
data class EntryMoodAnalysisResponse(
    val label: String? = null,
    val score: Float? = null,
    val confidence: Float? = null,
    val explanation: String? = null,
)

@Serializable
data class JournalEntryResponse(
    val id: String,
    @SerialName("recording_session_id")
    val recordingSessionId: String? = null,
    val title: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("recorded_at")
    val recordedAt: String? = null,
    @Serializable(with = EntryTranscriptResponseSerializer::class)
    val transcript: EntryTranscriptResponse? = null,
    val tags: List<EntryTagResponse> = emptyList(),
    @SerialName("mood_analysis")
    val moodAnalysis: EntryMoodAnalysisResponse? = null,
    val takeaway: String? = null,
    val summary: String? = null,
    val highlights: List<String> = emptyList(),
    @SerialName("audio_url")
    val audioUrl: String? = null,
    @SerialName("audio_signed_url")
    val audioSignedUrl: String? = null,
)

@Serializable
data class JournalEntrySummaryResponse(
    val id: String,
    @SerialName("entry_id")
    val entryId: String? = null,
    val title: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val summary: String? = null,
    val status: String? = null,
    @SerialName("mood_label")
    val moodLabel: String? = null,
    @SerialName("audio_url")
    val audioUrl: String? = null,
)

@Serializable
data class JournalEntriesResponse(
    val entries: List<JournalEntrySummaryResponse> = emptyList(),
    val limit: Int? = null,
    val offset: Int? = null,
    val total: Int? = null,
)

data class ArchiveEntrySummary(
    val id: String,
    val title: String,
    val createdAtMillis: Long,
    val summary: String,
    val status: String,
    val moodLabel: String? = null,
)

fun JournalEntryResponse.toJournalEntry(): JournalEntry {
    val createdAtMillis = createdAt?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis()
    val recordedAtMillis = recordedAt?.let { parseIso8601ToMillis(it) } ?: createdAtMillis
    val transcriptRecordingSessionId = recordingSessionId ?: "unknown"

    return JournalEntry(
        id = id,
        recordingSessionId = transcriptRecordingSessionId,
        title = title,
        createdAtMillis = createdAtMillis,
        recordedAtMillis = recordedAtMillis,
        transcript = Transcript(
            id = "transcript-$id",
            recordingSessionId = transcriptRecordingSessionId,
            fullText = transcript?.fullText.orEmpty(),
        ),
        tags = tags.map { JournalTag(name = it.name ?: it.label.orEmpty()) },
        audioAsset = (audioSignedUrl ?: audioUrl)?.let { url ->
            AudioAsset(
                id = "asset-$id",
                recordingSessionId = transcriptRecordingSessionId,
                remoteUrl = url,
                uploadState = AudioUploadState.Uploaded
            )
        },
        moodAnalysis = moodAnalysis?.let {
            val label = it.label ?: "Reflection"
            MoodAnalysis(
                label = label,
                score = it.score ?: 0f,
                confidence = it.confidence,
                explanation = it.explanation,
            )
        },
        takeaway = takeaway ?: summary,
        status = JournalEntryStatus.Ready,
    )
}

fun JournalEntrySummaryResponse.toArchiveEntrySummary(): ArchiveEntrySummary {
    val createdAtMillis = createdAt?.let { parseIso8601ToMillis(it) } ?: System.currentTimeMillis()
    return ArchiveEntrySummary(
        id = entryId ?: id,
        title = title.orEmpty().ifBlank { "Journal entry" },
        createdAtMillis = createdAtMillis,
        summary = summary.orEmpty(),
        status = status.orEmpty(),
        moodLabel = moodLabel,
    )
}

fun JournalEntry.toArchiveEntrySummary(): ArchiveEntrySummary {
    return ArchiveEntrySummary(
        id = id,
        title = title.orEmpty().ifBlank { "Journal entry" },
        createdAtMillis = createdAtMillis,
        summary = takeaway.orEmpty(),
        status = status.name,
        moodLabel = moodAnalysis?.label
    )
}

internal fun parseIso8601ToMillis(value: String): Long {
    return runCatching {
        java.time.Instant.parse(value).toEpochMilli()
    }.getOrElse {
        System.currentTimeMillis()
    }
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

@Serializable
data class ProfileResponse(
    val user_id: String? = null,
    val email: String? = null,
    val full_name: String? = null,
    val display_name: String? = null,
    val name: String? = null,
    val avatar_url: String? = null,
    val bio: String? = null,
    val streak_count: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null,
) {
    // Compatibility accessors
    val display_name_compat: String? 
        get() = full_name?.takeIf { it.isNotBlank() } 
            ?: display_name?.takeIf { it.isNotBlank() } 
            ?: name?.takeIf { it.isNotBlank() }
            
    val tagline: String? get() = bio
    val initials: String?
        get() = runCatching {
            display_name_compat?.split(" ")
                ?.mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                ?.take(2)
                ?.joinToString("")
        }.getOrNull()

    // Backwards-compatible default fields used by the Android app UI
    val entry_count: Int get() = 0
    val voice_minutes: Int get() = 0
    val milestones: List<String> get() = emptyList()
    val next_milestone: String? get() = null
}

@Serializable
data class PreferencesResponse(
    val theme: String = "light",
    val notifications_enabled: Boolean = true,
    val reminder_time: String? = "08:00",
    val language: String = "en",
) {
    // Compatibility accessors for app preferences
    val appearance_mode: String get() = theme
    val prompt_reminder_time: String? get() = reminder_time

    // Backwards-compatible audio quality used by UI
    val audio_quality: String get() = "high"
}

@Serializable
data class UpdatePreferencesRequest(
    val appearance_mode: String? = null,
    val notifications_enabled: Boolean? = null,
    val prompt_reminder_time: String? = null,
    val audio_quality: String? = null,
    val language: String? = null
)

@Serializable
data class DashboardResponse(
    val prompt: String? = null,
    val prompt_status: String = "unavailable",
    val recent_entries: List<JournalEntrySummaryResponse> = emptyList(),
    val streak_count: Int = 0,
    val entry_count: Int = 0,
)

package com.example.thoughts

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstract interface for turning audio into text.
 * Part of the "Engine Pattern" to allow swapping transcription models.
 */
interface TranscriptionEngine {
    /**
     * Current state of the transcription (text, errors, status).
     */
    val state: StateFlow<TranscriptionState>

    /**
     * Start the transcription process.
     */
    fun start(languageCode: String = "en-US")

    /**
     * Stop the transcription process.
     */
    fun stop()

    /**
     * Cancel the current transcription and discard results.
     */
    fun cancel()

    /**
     * Release resources.
     */
    fun release()
}

/**
 * Represents the current state of a transcription session.
 */
data class TranscriptionState(
    val isTranscribing: Boolean = false,
    val transcript: String = "",
    val error: String? = null,
    val errorCode: Int? = null
)

package com.example.thoughts

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "SystemTranscriptionEngine"

/**
 * Implementation of [TranscriptionEngine] using Android's built-in [SpeechRecognizer].
 */
class SystemTranscriptionEngine(
    private val app: Application
) : TranscriptionEngine, RecognitionListener {

    private val _state = MutableStateFlow(TranscriptionState())
    override val state: StateFlow<TranscriptionState> = _state.asStateFlow()

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(app)
    private var isRecognizerActive = false

    override fun start(languageCode: String) {
        if (isRecognizerActive) {
            Log.d(TAG, "Recognizer already active, ignoring start")
            return
        }
        
        _state.update { it.copy(error = null, errorCode = null) }

        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update {
                it.copy(
                    error = "Recognition is not available on this device"
                )
            }
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, app.packageName)
        }

        recognizer.setRecognitionListener(this)
        try {
            recognizer.startListening(intent)
            isRecognizerActive = true
            _state.update {
                it.copy(
                    isTranscribing = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _state.update { it.copy(error = e.message, isTranscribing = false) }
        }
    }

    override fun stop() {
        if (!isRecognizerActive) return
        
        _state.update {
            it.copy(
                isTranscribing = false
            )
        }
        recognizer.stopListening()
    }

    override fun cancel() {
        recognizer.cancel()
        isRecognizerActive = false
        _state.update { it.copy(isTranscribing = false) }
    }

    override fun release() {
        recognizer.setRecognitionListener(null)
        recognizer.cancel()
        recognizer.destroy()
        isRecognizerActive = false
    }

    // --- RecognitionListener ---

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null) }
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        _state.update { it.copy(isTranscribing = false) }
        isRecognizerActive = false
    }

    override fun onError(error: Int) {
        isRecognizerActive = false
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable"
            else -> "Unknown error: $error"
        }
        
        Log.e(TAG, "Speech recognition error: $errorMessage ($error)")
        _state.update {
            it.copy(
                error = errorMessage,
                errorCode = error,
                isTranscribing = false
            )
        }
    }

    override fun onResults(results: Bundle?) {
        isRecognizerActive = false
        results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.getOrNull(0)
            ?.let { result ->
                _state.update {
                    it.copy(
                        transcript = result
                    )
                }
            }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.getOrNull(0)
            ?.let { result ->
                _state.update {
                    it.copy(
                        transcript = result
                    )
                }
            }
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

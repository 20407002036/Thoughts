package com.example.thoughts

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val TAG = "VoiceToTextParser"

data class VoiceToTextParserState(
    val isSpeaking: Boolean = false,
    val spokenText: String = "",
    val error: String? = null,
    val errorCode: Int? = null
)

class VoiceToTextParser(
    private val app: Application
) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextParserState())
    val state = _state.asStateFlow()

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(app)
    private var isRecognizerActive = false

    fun startListening(languageCode: String = "en-US") {
        if (isRecognizerActive) {
            Log.d(TAG, "Recognizer already active, ignoring startListening")
            return
        }
        
        _state.update { it.copy(error = null, errorCode = null) }

        if (!SpeechRecognizer.isRecognitionAvailable(app)) {
            _state.update {
                it.copy(
                    error = "Recognition is not available"
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
                    isSpeaking = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            _state.update { it.copy(error = e.message, isSpeaking = false) }
        }
    }

    fun stopListening() {
        if (!isRecognizerActive) return
        
        _state.update {
            it.copy(
                isSpeaking = false
            )
        }
        recognizer.stopListening()
        // We don't set isRecognizerActive = false here, 
        // we wait for onResults, onError, or onEndOfSpeech
    }

    fun cancel() {
        recognizer.cancel()
        isRecognizerActive = false
        _state.update { it.copy(isSpeaking = false) }
    }

    fun release() {
        recognizer.setRecognitionListener(null)
        recognizer.cancel()
        recognizer.destroy()
        isRecognizerActive = false
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _state.update { it.copy(error = null) }
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        _state.update { it.copy(isSpeaking = false) }
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
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language unavailable (SODA Error 13)"
            else -> "Unknown error: $error"
        }
        
        Log.e(TAG, "Speech recognition error: $errorMessage ($error)")
        _state.update {
            it.updateError(errorMessage, error)
        }
    }

    private fun VoiceToTextParserState.updateError(message: String, code: Int) = copy(
        error = message,
        errorCode = code,
        isSpeaking = false
    )

    override fun onResults(results: Bundle?) {
        isRecognizerActive = false
        results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.getOrNull(0)
            ?.let { result ->
                _state.update {
                    it.copy(
                        spokenText = result
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
                        spokenText = result
                    )
                }
            }
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}

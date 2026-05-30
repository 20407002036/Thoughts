package com.example.thoughts

import android.util.Log
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

private const val TAG = "LiveTranscriptionWS"

@Serializable
data class LiveTranscriptionMessage(
    @SerialName("partial") val partial: String? = null,
    @SerialName("final") val finalText: String? = null,
    @SerialName("is_final") val isFinal: Boolean? = null,
    @SerialName("session_ended") val sessionEnded: Boolean? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("code") val code: String? = null,
)

class LiveTranscriptionWebSocketClient(
    private val url: String,
    private val authorizationHeader: String?,
    private val onOpen: () -> Unit,
    private val onMessage: (LiveTranscriptionMessage) -> Unit,
    private val onFailure: (Throwable) -> Unit,
    private val onClosed: () -> Unit,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var webSocket: WebSocket? = null

    fun connect() {
        if (webSocket != null) return

        val requestBuilder = Request.Builder().url(url)
        if (!authorizationHeader.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", authorizationHeader)
        }

        val request = requestBuilder.build()
        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket opened")
                    onOpen()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val msg = json.decodeFromString(LiveTranscriptionMessage.serializer(), text)
                        onMessage(msg)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse WS message: $text", e)
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    // Server should only send text JSON. Ignore binary.
                    Log.w(TAG, "Ignoring unexpected binary WS message (${bytes.size} bytes)")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure (code=${response?.code})", t)
                    this@LiveTranscriptionWebSocketClient.webSocket = null
                    onFailure(t)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing (code=$code, reason=$reason)")
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed (code=$code, reason=$reason)")
                    this@LiveTranscriptionWebSocketClient.webSocket = null
                    onClosed()
                }
            },
        )
    }

    fun sendPcm(bytes: ByteArray, length: Int) {
        val ws = webSocket ?: return
        if (length <= 0) return
        try {
            ws.send(bytes.toByteString(0, length))
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending PCM chunk", e)
        }
    }

    fun stop() {
        // Protocol: send text "stop" to finalize.
        try {
            webSocket?.send("stop")
        } catch (e: Exception) {
            Log.e(TAG, "Failed sending stop", e)
        }
    }

    fun close(code: Int = 1000, reason: String = "client_close") {
        try {
            webSocket?.close(code, reason)
        } catch (e: Exception) {
            Log.e(TAG, "Failed closing WS", e)
        } finally {
            webSocket = null
        }
    }
}

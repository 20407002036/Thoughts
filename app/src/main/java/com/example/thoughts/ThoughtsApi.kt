package com.example.thoughts

import java.net.URI

object ThoughtsApi {
    // Specify your backend server URL here
    const val BASE_URL = "https://stockily-intramarginal-tonja.ngrok-free.dev/"

    const val AUTH_LOGIN_ENDPOINT = "${BASE_URL}v1/auth/login"
    const val AUTH_SIGN_UP_ENDPOINT = "${BASE_URL}v1/auth/signup"
    const val AUTH_REFRESH_ENDPOINT = "${BASE_URL}v1/auth/refresh"
    const val AUTH_LOGOUT_ENDPOINT = "${BASE_URL}v1/auth/logout"
    
    // Example endpoint for audio upload
    const val UPLOAD_AUDIO_ENDPOINT = "${BASE_URL}v1/journals/ingest"

    fun liveTranscribeWebSocketUrl(): String {
        val base = BASE_URL.trim()
        val uri = URI(base)
        val scheme = when (uri.scheme?.lowercase()) {
            "http" -> "ws"
            "https" -> "wss"
            "ws", "wss" -> uri.scheme.lowercase()
            else -> "wss"
        }

        val host = uri.host ?: uri.authority
        val portPart = if (uri.port != -1) ":${uri.port}" else ""
        val basePath = (uri.path ?: "").trimEnd('/')
        return "$scheme://$host$portPart$basePath/v1/journals/live-transcribe"
    }
}

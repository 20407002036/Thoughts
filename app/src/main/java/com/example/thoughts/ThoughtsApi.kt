package com.example.thoughts

object ThoughtsApi {
    // Specify your backend server URL here
    const val BASE_URL = "https://stockily-intramarginal-tonja.ngrok-free.dev"

    const val AUTH_LOGIN_ENDPOINT = "$BASE_URL/v1/auth/login"
    const val AUTH_SIGN_UP_ENDPOINT = "$BASE_URL/v1/auth/signup"
    const val AUTH_REFRESH_ENDPOINT = "$BASE_URL/v1/auth/refresh"
    const val AUTH_LOGOUT_ENDPOINT = "$BASE_URL/v1/auth/logout"
    
    // Example endpoint for audio upload
    const val UPLOAD_AUDIO_ENDPOINT = "$BASE_URL/v1/journals/ingest"
}

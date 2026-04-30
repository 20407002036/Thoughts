package com.example.thoughts

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthSessionManager {
    private const val PreferencesName = "auth_session"
    private const val AccessTokenKey = "access_token"
    private const val RefreshTokenKey = "refresh_token"
    private const val UserIdKey = "user_id"
    private const val EmailKey = "email"
    private const val DisplayNameKey = "display_name"

    private val sessionState = MutableStateFlow<AuthSession?>(null)
    private var preferences: SharedPreferences? = null

    val session: StateFlow<AuthSession?> = sessionState.asStateFlow()

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
        sessionState.value = readSession()
    }

    fun saveSession(session: AuthSession) {
        val prefs = checkNotNull(preferences) { "AuthSessionManager must be initialized before saving a session." }
        prefs.edit()
            .putString(AccessTokenKey, session.accessToken)
            .putString(RefreshTokenKey, session.refreshToken)
            .putString(UserIdKey, session.userId)
            .putString(EmailKey, session.email)
            .putString(DisplayNameKey, session.displayName)
            .apply()
        sessionState.value = session
    }

    fun clearSession() {
        val prefs = checkNotNull(preferences) { "AuthSessionManager must be initialized before clearing a session." }
        prefs.edit().clear().apply()
        sessionState.value = null
    }

    fun authorizationHeader(): String? {
        val token = sessionState.value?.accessToken?.trim().orEmpty()
        return token.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
    }

    private fun readSession(): AuthSession? {
        val prefs = preferences ?: return null
        val accessToken = prefs.getString(AccessTokenKey, null).orEmpty()
        if (accessToken.isBlank()) return null
        return AuthSession(
            accessToken = accessToken,
            refreshToken = prefs.getString(RefreshTokenKey, null),
            userId = prefs.getString(UserIdKey, null),
            email = prefs.getString(EmailKey, null),
            displayName = prefs.getString(DisplayNameKey, null),
        )
    }
}
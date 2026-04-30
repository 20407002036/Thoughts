package com.example.thoughts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface AuthApiService {
    @POST("v1/auth/login")
    suspend fun login(@Body request: AuthLoginRequest): ResponseBody

    @POST("v1/auth/signup")
    suspend fun signUp(@Body request: AuthSignUpRequest): ResponseBody

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: AuthRefreshRequest): ResponseBody

    @POST("v1/auth/logout")
    suspend fun logout(@Body request: AuthLogoutRequest): ResponseBody
}

object AuthRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ThoughtsApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(AuthApiService::class.java)

    suspend fun login(email: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            parseSession(api.login(AuthLoginRequest(email = email.trim(), password = password)).string())
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            parseSession(
                api.signUp(
                    AuthSignUpRequest(
                        email = email.trim(),
                        password = password,
                        displayName = displayName.trim().ifBlank { null },
                    )
                ).string()
            )
        }
    }

    suspend fun refresh(refreshToken: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            parseSession(api.refresh(AuthRefreshRequest(refreshToken = refreshToken.trim())).string())
        }
    }

    suspend fun logout(session: AuthSession?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val currentSession = session ?: return@runCatching Unit
            api.logout(
                AuthLogoutRequest(
                    accessToken = currentSession.accessToken,
                    refreshToken = currentSession.refreshToken,
                )
            ).string()
            Unit
        }
    }

    private fun parseSession(body: String): AuthSession {
        val element = runCatching { json.parseToJsonElement(body) }.getOrNull()
        if (element == null) {
            throw IllegalStateException("Authentication succeeded, but the server response could not be parsed.")
        }

        val root = element.jsonObjectOrNull() ?: throw IllegalStateException(
            "Authentication succeeded, but the server response was not a JSON object."
        )

        val data = root["data"]?.jsonObjectOrNull() ?: root
        val token = stringValue(data, "accessToken", "access_token", "token", "jwt", "sessionToken", "session_token")
            ?: stringValue(root, "accessToken", "access_token", "token", "jwt", "sessionToken", "session_token")
            ?: throw IllegalStateException("Authentication succeeded, but no token was returned by the server.")

        return AuthSession(
            accessToken = token,
            refreshToken = stringValue(data, "refreshToken", "refresh_token")
                ?: stringValue(root, "refreshToken", "refresh_token"),
            userId = stringValue(data, "userId", "user_id", "id")
                ?: stringValue(root, "userId", "user_id", "id"),
            email = stringValue(data, "email") ?: stringValue(root, "email"),
            displayName = stringValue(data, "displayName", "display_name", "name")
                ?: stringValue(root, "displayName", "display_name", "name"),
        )
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = runCatching { jsonObject }.getOrNull()

    private fun stringValue(source: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val value = source[key]
            val string = (value as? JsonPrimitive)?.content
            if (!string.isNullOrBlank()) {
                return string
            }
        }
        return null
    }
}
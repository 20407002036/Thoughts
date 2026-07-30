
package com.example.thoughts

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.MultipartBody
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "BackendService"

// Retrofit Interface for backend communication
interface JournalApiService {
    
    @Multipart
    @POST("v1/journals/ingest")
    suspend fun uploadAudioForTranscription(
        @Header("Authorization") authorization: String,
        @Part audio: MultipartBody.Part,
    ): IngestionResponse

    @retrofit2.http.GET("v1/journals")
    suspend fun listJournalEntries(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Query("limit") limit: Int = 20,
        @retrofit2.http.Query("offset") offset: Int = 0,
    ): JournalEntriesResponse

    @retrofit2.http.GET("v1/journals/{id}")
    suspend fun getJournalEntry(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Path("id") id: String,
    ): JournalEntryResponse

    @retrofit2.http.GET("v1/profile")
    suspend fun getProfile(
        @Header("Authorization") authorization: String,
    ): ProfileResponse

    @retrofit2.http.GET("v1/dashboard")
    suspend fun getDashboard(
        @Header("Authorization") authorization: String,
    ): DashboardResponse

    @retrofit2.http.GET("v1/preferences")
    suspend fun getPreferences(
        @Header("Authorization") authorization: String,
    ): PreferencesResponse

    @retrofit2.http.PATCH("v1/preferences")
    suspend fun updatePreferences(
        @Header("Authorization") authorization: String,
        @retrofit2.http.Body request: UpdatePreferencesRequest,
    ): PreferencesResponse
}

// Singleton BackendService
object BackendService {
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }
    
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
            redactHeader("Authorization")
        })
        .addNetworkInterceptor(Interceptor { chain ->
            val originalResponse = chain.proceed(chain.request())
            Log.d(TAG, "Response code: ${originalResponse.code}")
            originalResponse
        })
        .build()
    
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ThoughtsApi.BASE_URL)
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val apiService: JournalApiService = retrofit.create(JournalApiService::class.java)

    private val refreshMutex = Mutex()

    private suspend fun <T> authCall(
        errorMessage: String,
        block: suspend (authorization: String) -> T,
    ): Result<T> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: throw IllegalStateException("User session not found")
            Result.success(block(authorization))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val authorization = AuthSessionManager.authorizationHeader()
                        ?: throw IllegalStateException("User session not found")
                    Result.success(block(authorization))
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, errorMessage, e)
            Result.failure(e)
        }
    }

    suspend fun uploadAudioForTranscription(
        audioFile: File,
        durationMs: Long,
        locale: String = "en-US",
    ): Result<IngestionResponse> {
        return authCall("Upload failed") {
            uploadAudioOnce(audioFile)
        }
    }

    suspend fun listJournalEntries(
        limit: Int = 20,
        offset: Int = 0,
    ): Result<JournalEntriesResponse> {
        return authCall("Failed to list journal entries") { authorization ->
            apiService.listJournalEntries(authorization, limit, offset)
        }
    }

    suspend fun getJournalEntry(id: String): Result<JournalEntryResponse> {
        return authCall("Failed to get journal entry: $id") { authorization ->
            apiService.getJournalEntry(authorization, id)
        }
    }

    suspend fun getProfile(): Result<ProfileResponse> {
        return authCall("Failed to get profile") { authorization ->
            apiService.getProfile(authorization)
        }
    }

    suspend fun getDashboard(): Result<DashboardResponse> {
        return authCall("Failed to get dashboard") { authorization ->
            apiService.getDashboard(authorization)
        }
    }

    suspend fun getPreferences(): Result<PreferencesResponse> {
        return authCall("Failed to get preferences") { authorization ->
            apiService.getPreferences(authorization)
        }
    }

    suspend fun updatePreferences(request: UpdatePreferencesRequest): Result<PreferencesResponse> {
        return authCall("Failed to update preferences") { authorization ->
            apiService.updatePreferences(authorization, request)
        }
    }

    private suspend fun uploadAudioOnce(
        audioFile: File,
    ): IngestionResponse {
        Log.d(TAG, "Uploading audio: ${audioFile.name} (${audioFile.length()} bytes)")

        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: ${audioFile.absolutePath}")
            throw IllegalArgumentException("Audio file not found")
        }

        val authorization = AuthSessionManager.authorizationHeader()
            ?: throw IllegalStateException("Sign in before uploading your recording.")

        val mediaType = when (audioFile.extension.lowercase()) {
            "wav" -> "audio/wav"
            "m4a" -> "audio/m4a"
            else -> "application/octet-stream"
        }
        val requestBody = audioFile.asRequestBody(mediaType.toMediaType())
        val part = MultipartBody.Part.createFormData("audio", audioFile.name, requestBody)

        val response = apiService.uploadAudioForTranscription(
            authorization = authorization,
            audio = part,
        )

        Log.d(TAG, "Upload successful. Transcript length: ${response.transcript.length}")
        return response
    }

    private suspend fun refreshSessionIfNeeded(): Boolean {
        val session = AuthSessionManager.session.value ?: return false
        val refreshToken = session.refreshToken?.trim().orEmpty()
        if (refreshToken.isBlank()) return false

        return refreshMutex.withLock {
            val existingToken = AuthSessionManager.authorizationHeader()
            if (!existingToken.isNullOrBlank()) return@withLock true

            val result = AuthRepository.refresh(refreshToken)
                .onSuccess { AuthSessionManager.saveSession(it) }
                .onFailure { throwable ->
                    if (throwable is HttpException && (throwable.code() == 401 || throwable.code() == 403)) {
                        AuthSessionManager.clearSession()
                    }
                }

            if (!result.isSuccess) return@withLock false

            val savedToken = AuthSessionManager.authorizationHeader()
            if (savedToken.isNullOrBlank()) {
                Log.e(TAG, "Token refresh returned empty token")
                AuthSessionManager.clearSession()
                return@withLock false
            }

            true
        }
    }
}

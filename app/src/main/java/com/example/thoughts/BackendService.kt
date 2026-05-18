
package com.example.thoughts

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Header
import retrofit2.http.Multipart
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
    
    /**
     * Upload audio file for transcription and analysis.
     * 
     * @param audioFile The local audio file to upload
     * @param durationMs Duration of the recording in milliseconds
     * @param locale Language locale (e.g., "en-US")
     * @return IngestionResponse containing transcript, mood analysis, and tags from backend
     */
    suspend fun uploadAudioForTranscription(
        audioFile: File,
        durationMs: Long,
        locale: String = "en-US",
    ): Result<IngestionResponse> {
        return try {
            Result.success(uploadAudioOnce(audioFile))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    Result.success(uploadAudioOnce(audioFile))
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun listJournalEntries(
        limit: Int = 20,
        offset: Int = 0,
    ): Result<JournalEntriesResponse> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: throw IllegalStateException("User session not found")
            Result.success(apiService.listJournalEntries(authorization, limit, offset))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val authorization = AuthSessionManager.authorizationHeader()
                        ?: throw IllegalStateException("User session not found")
                    Result.success(apiService.listJournalEntries(authorization, limit, offset))
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list journal entries", e)
            Result.failure(e)
        }
    }

    suspend fun getJournalEntry(id: String): Result<JournalEntryResponse> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: throw IllegalStateException("User session not found")
            Result.success(apiService.getJournalEntry(authorization, id))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val authorization = AuthSessionManager.authorizationHeader()
                        ?: throw IllegalStateException("User session not found")
                    Result.success(apiService.getJournalEntry(authorization, id))
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get journal entry: $id", e)
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<ProfileResponse> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: throw IllegalStateException("User session not found")
            Result.success(apiService.getProfile(authorization))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val authorization = AuthSessionManager.authorizationHeader()
                        ?: throw IllegalStateException("User session not found")
                    Result.success(apiService.getProfile(authorization))
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get profile", e)
            Result.failure(e)
        }
    }

    suspend fun getDashboard(): Result<DashboardResponse> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: throw IllegalStateException("User session not found")
            Result.success(apiService.getDashboard(authorization))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val authorization = AuthSessionManager.authorizationHeader()
                        ?: throw IllegalStateException("User session not found")
                    Result.success(apiService.getDashboard(authorization))
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get dashboard", e)
            Result.failure(e)
        }
    }

    suspend fun getPreferences(): Result<PreferencesResponse> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: throw IllegalStateException("User session not found")
            Result.success(apiService.getPreferences(authorization))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val authorization = AuthSessionManager.authorizationHeader()
                        ?: throw IllegalStateException("User session not found")
                    Result.success(apiService.getPreferences(authorization))
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get preferences", e)
            Result.failure(e)
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

        val requestBody = audioFile.asRequestBody("audio/m4a".toMediaType())
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

        return AuthRepository.refresh(refreshToken)
            .onSuccess { refreshedSession ->
                AuthSessionManager.saveSession(refreshedSession)
            }
            .onFailure { throwable ->
                if (throwable is HttpException && (throwable.code() == 401 || throwable.code() == 403)) {
                    AuthSessionManager.clearSession()
                }
            }
            .isSuccess
    }
}

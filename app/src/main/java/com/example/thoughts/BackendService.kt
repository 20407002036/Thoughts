
package com.example.thoughts

import android.util.Log
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import okhttp3.MultipartBody
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "BackendService"

// Retrofit Interface for backend communication
interface JournalApiService {
    
    @Multipart
    @POST("v1/recordings")
    suspend fun uploadRecording(
        @Header("Authorization") authorization: String,
        @Part audio: MultipartBody.Part,
        @Part("duration_ms") durationMs: String,
        @Part("locale") locale: String = "en-US",

    ): RecordingUploadResponse

    @GET("v1/entries/{entryId}")
    suspend fun getEntry(
        @Header("Authorization") authorization: String,
        @Path("entryId") entryId: String,
    ): JournalEntryResponse
    
    @GET("v1/entries")
    suspend fun getEntries(
        @Header("Authorization") authorization: String,
    ): JournalEntriesResponse
}

// Singleton BackendService
object BackendService {
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
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
    ): Result<RecordingUploadResponse> {
        return try {
            Result.success(uploadAudioOnce(audioFile, durationMs, locale))
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    Result.success(uploadAudioOnce(audioFile, durationMs, locale))
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

    private suspend fun uploadAudioOnce(
        audioFile: File,
        durationMs: Long,
        locale: String,
    ): RecordingUploadResponse {
        Log.d(TAG, "Uploading audio: ${audioFile.name} (${audioFile.length()} bytes, ${durationMs}ms)")

        if (!audioFile.exists()) {
            Log.e(TAG, "Audio file does not exist: ${audioFile.absolutePath}")
            throw IllegalArgumentException("Audio file not found")
        }

        val authorization = AuthSessionManager.authorizationHeader()
            ?: throw IllegalStateException("Sign in before uploading your recording.")

        val requestBody = audioFile.asRequestBody("audio/m4a".toMediaType())
        val part = MultipartBody.Part.createFormData("audio", audioFile.name, requestBody)

        val response = apiService.uploadRecording(
            authorization = authorization,
            audio = part,
            durationMs = durationMs.toString(),
            locale = locale,
        )

        Log.d(TAG, "Upload successful. Recording id: ${response.recordingId}, entry id: ${response.entryId}")
        return response
    }

    suspend fun fetchArchivedEntries(): Result<List<ArchiveEntrySummary>> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: return Result.failure(IllegalStateException("Sign in before loading your archives."))

            val response = apiService.getEntries(authorization = authorization)
            Result.success(response.entries.map { it.toArchiveEntrySummary() })
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val retryAuthorization = AuthSessionManager.authorizationHeader()
                        ?: return Result.failure(IllegalStateException("Sign in before loading your archives."))
                    val retryResponse = apiService.getEntries(authorization = retryAuthorization)
                    Result.success(retryResponse.entries.map { it.toArchiveEntrySummary() })
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch archive entries", e)
            Result.failure(e)
        }
    }

    suspend fun fetchEntry(entryId: String): Result<JournalEntry> {
        return try {
            val authorization = AuthSessionManager.authorizationHeader()
                ?: return Result.failure(IllegalStateException("Sign in before loading a journal entry."))

            val response = apiService.getEntry(
                authorization = authorization,
                entryId = entryId,
            )
            Result.success(response.toJournalEntry())
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshSessionIfNeeded()) {
                try {
                    val retryAuthorization = AuthSessionManager.authorizationHeader()
                        ?: return Result.failure(IllegalStateException("Sign in before loading a journal entry."))
                    val retryResponse = apiService.getEntry(
                        authorization = retryAuthorization,
                        entryId = entryId,
                    )
                    Result.success(retryResponse.toJournalEntry())
                } catch (retryError: Exception) {
                    Result.failure(retryError)
                }
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch entry", e)
            Result.failure(e)
        }
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

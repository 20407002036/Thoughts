
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
        @Part("duration_ms") durationMs: String,
        @Part("locale") locale: String = "en-US",
    ): IngestionResponse
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
    ): Result<IngestionResponse> {
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
    ): IngestionResponse {
        Log.d(TAG, "Uploading audio: ${audioFile.name} (${audioFile.length()} bytes, ${durationMs}ms)")

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
            durationMs = durationMs.toString(),
            locale = locale,
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

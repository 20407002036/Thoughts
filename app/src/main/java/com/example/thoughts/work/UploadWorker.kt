package com.example.thoughts.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.thoughts.AuthSessionManager
import com.example.thoughts.AudioUploadState
import com.example.thoughts.BackendService
import com.example.thoughts.JournalRepository
import java.io.File
import java.util.Locale

class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UploadWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Background upload worker started")
        AuthSessionManager.initialize(applicationContext)
        JournalRepository.ensureInitialized(applicationContext)

        try {
            // 1. Fetch assets that need uploading
            // We use the repository which is already initialized in MainActivity
            val pendingAssets = JournalRepository.getAssetsToUpload()

            if (pendingAssets.isEmpty()) {
                Log.d(TAG, "No pending assets to upload")
                return Result.success()
            }

            Log.d(TAG, "Found ${pendingAssets.size} pending assets to upload")

            var hasPermanentFailure = false
            var hasTransientFailure = false

            for (asset in pendingAssets) {
                val filePath = asset.localPath
                if (filePath == null) {
                    Log.e(TAG, "Audio file path missing for asset ${asset.id}")
                    JournalRepository.updateAudioUploadState(asset.id, AudioUploadState.Failed)
                    hasPermanentFailure = true
                    continue
                }
                val file = File(filePath)

                if (!file.exists()) {
                    Log.e(TAG, "Audio file missing for asset ${asset.id}: $filePath")
                    // Mark as failed since we can't upload a missing file
                    JournalRepository.updateAudioUploadState(asset.id, AudioUploadState.Failed)
                    hasPermanentFailure = true
                    continue
                }

                Log.d(TAG, "Attempting background upload for asset ${asset.id}...")
                JournalRepository.updateAudioUploadState(asset.id, AudioUploadState.Uploading)

                // Perform the upload
                val result = BackendService.uploadAudioForTranscription(
                    audioFile = file,
                    durationMs = asset.durationMs,
                    locale = "en-US" // Default locale
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Background upload successful for asset ${asset.id}")
                    JournalRepository.persistUploadResult(
                        asset = asset,
                        response = result.getOrThrow(),
                        languageTag = Locale.getDefault().toLanguageTag(),
                    )
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "Background upload failed for asset ${asset.id}: ${error?.message}")

                    JournalRepository.updateAudioUploadState(asset.id, AudioUploadState.Failed)

                    if (isTransientError(error)) {
                        hasTransientFailure = true
                    } else {
                        hasPermanentFailure = true
                    }
                }
            }

            return when {
                hasTransientFailure -> Result.retry()
                hasPermanentFailure -> Result.failure()
                else -> Result.success()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in UploadWorker", e)
            return Result.retry()
        }
    }

    private fun isTransientError(e: Throwable?): Boolean {
        // Retry on network issues or 5xx server errors
        return e is java.io.IOException || (e is retrofit2.HttpException && e.code() >= 500)
    }
}

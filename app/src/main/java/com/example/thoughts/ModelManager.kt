package com.example.thoughts

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

private const val TAG = "ModelManager"
private const val MODEL_FILENAME = "gemma-2b.bin"
private const val MODEL_SUBDIR = "models"

// Replace with actual URL for Gemma 2B MediaPipe task file (e.g. from Kaggle or HuggingFace)
private const val MODEL_URL = "https://example.com/models/gemma-2b-it-cpu-int4.bin"

sealed class ModelStatus {
    object NotDownloaded : ModelStatus()
    data class Downloading(val progress: Int) : ModelStatus()
    object Downloaded : ModelStatus()
    data class Error(val message: String) : ModelStatus()
}

/**
 * Manages the lifecycle of the on-device AI model files.
 */
class ModelManager(private val context: Context) {

    private val _status = MutableStateFlow<ModelStatus>(ModelStatus.NotDownloaded)
    val status = _status.asStateFlow()

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var downloadId: Long = -1L

    init {
        checkModelExists()
    }

    private fun checkModelExists() {
        if (getModelFile().exists()) {
            _status.value = ModelStatus.Downloaded
        } else {
            _status.value = ModelStatus.NotDownloaded
        }
    }

    fun getModelFile(): File {
        val modelsDir = File(context.filesDir, MODEL_SUBDIR)
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }
        return File(modelsDir, MODEL_FILENAME)
    }

    fun startDownload() {
        if (_status.value is ModelStatus.Downloaded || _status.value is ModelStatus.Downloading) return

        try {
            val request = DownloadManager.Request(Uri.parse(MODEL_URL))
                .setTitle("Downloading AI Brain")
                .setDescription("Fetching Gemma 2B model for local analysis")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setDestinationInExternalFilesDir(context, null, "$MODEL_SUBDIR/$MODEL_FILENAME")
                .setAllowedOverMetered(false) // Prefer Wi-Fi
                .setAllowedOverRoaming(false)

            downloadId = downloadManager.enqueue(request)
            _status.value = ModelStatus.Downloading(0)
            Log.d(TAG, "Started model download with ID: $downloadId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
            _status.value = ModelStatus.Error("Failed to start download: ${e.message}")
        }
    }

    /**
     * Call this periodically or via a broadcast receiver to update download progress.
     */
    fun updateProgress() {
        if (downloadId == -1L) return

        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)
        if (cursor.moveToFirst()) {
            val bytesDownloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val bytesTotal = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))

            when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    // Move file from external to internal if needed, or just update state
                    _status.value = ModelStatus.Downloaded
                    downloadId = -1L
                }
                DownloadManager.STATUS_FAILED -> {
                    _status.value = ModelStatus.Error("Download failed")
                    downloadId = -1L
                }
                DownloadManager.STATUS_RUNNING -> {
                    if (bytesTotal > 0) {
                        val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
                        _status.value = ModelStatus.Downloading(progress)
                    }
                }
            }
        }
        cursor.close()
    }

    fun deleteModel() {
        val file = getModelFile()
        if (file.exists()) {
            file.delete()
            _status.value = ModelStatus.NotDownloaded
            Log.d(TAG, "Model file deleted")
        }
    }
}

package com.example.thoughts

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "AudioFileManager"
private const val AUDIO_DIR = "thoughts_audio"
private const val AUDIO_PREFIX = "recording_"
private const val AUDIO_EXTENSION = ".m4a"

object AudioFileManager {
    
    /**
     * Get or create the audio directory for storing recordings.
     * Uses app's cache directory for temporary recordings.
     */
    fun getAudioDirectory(context: Context): File {
        val audioDir = File(context.cacheDir, AUDIO_DIR)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return audioDir
    }
    
    /**
     * Create a new audio file with timestamp.
     * Returns a File ready to be written to by AudioRecorder.
     */
    fun createAudioFile(context: Context): File {
        val audioDir = getAudioDirectory(context)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "$AUDIO_PREFIX$timestamp$AUDIO_EXTENSION"
        return File(audioDir, fileName).also {
            Log.d(TAG, "Created audio file: ${it.absolutePath}")
        }
    }
    
    /**
     * Delete an audio file.
     * Safe to call even if file doesn't exist.
     */
    fun deleteAudioFile(file: File) {
        try {
            if (file.exists()) {
                file.delete()
                Log.d(TAG, "Deleted audio file: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete audio file: ${file.absolutePath}", e)
        }
    }
    
    /**
     * Get the size of an audio file in bytes.
     */
    fun getAudioFileSize(file: File): Long {
        return if (file.exists()) file.length() else 0L
    }
    
    /**
     * Cleanup all old audio files in the directory.
     * Useful for maintenance or clearing cache.
     */
    fun cleanupAudioDirectory(context: Context, maxAgeMs: Long = 24 * 60 * 60 * 1000) {
        val audioDir = getAudioDirectory(context)
        val now = System.currentTimeMillis()
        
        audioDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() + maxAgeMs < now) {
                deleteAudioFile(file)
            }
        }
    }
}

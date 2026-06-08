package com.example.thoughts

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private const val TAG = "MediaPipeAnalysisEngine"
private const val RELEASE_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes

/**
 * Implementation of [AnalysisEngine] using MediaPipe LLM Inference (Gemma 2B).
 * Includes Phase 4 optimizations for Resource & Power Management.
 */
class MediaPipeAnalysisEngine(
    private val context: Context,
    private val modelPath: String
) : AnalysisEngine {

    private var llmInference: LlmInference? = null
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Default)
    private var releaseJob: Job? = null

    init {
        checkResourceTiers()
    }

    private fun checkResourceTiers() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamGb = memoryInfo.totalMem / (1024 * 1024 * 1024.0)
        Log.d(TAG, "Device Total RAM: ${"%.2f".format(totalRamGb)} GB")
        
        if (totalRamGb < 8.0) {
            Log.w(TAG, "Device RAM is below 8GB. AI analysis may be slow or cause app restarts.")
        }
    }

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            resetReleaseTimer()
            if (llmInference != null) return@withContext Result.success(Unit)

            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                return@withContext Result.failure(Exception("Model file not found at $modelPath"))
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .setTopK(40)
                .setTemperature(0.7f)
                .setRandomSeed(System.currentTimeMillis().toInt())
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.d(TAG, "MediaPipe LLM Inference initialized successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe LLM", e)
            Result.failure(e)
        }
    }

    override suspend fun analyze(text: String): Result<AnalysisResult> = withContext(Dispatchers.IO) {
        resetReleaseTimer()
        
        // Auto-initialize if released
        if (llmInference == null) {
            val initResult = initialize()
            if (initResult.isFailure) {
                return@withContext Result.failure(initResult.exceptionOrNull()!!)
            }
        }

        val inference = llmInference ?: return@withContext Result.failure(Exception("Engine failed to initialize"))

        try {
            val prompt = buildPrompt(text)
            val response = inference.generateResponse(prompt)
            
            Log.d(TAG, "LLM Raw Response: $response")
            
            val cleanJson = extractJson(response)
            val output = jsonParser.decodeFromString<LlmAnalysisOutput>(cleanJson)
            
            Result.success(
                AnalysisResult(
                    tags = output.tags,
                    mood = output.mood,
                    moodScore = output.moodScore,
                    moodExplanation = output.moodExplanation,
                    takeaway = output.takeaway
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Analysis failed", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(text: String): String {
        return """
            You are a helpful AI journaling assistant. Analyze the following journal entry and provide structured feedback.
            Your output must be a single, valid JSON object with EXACTLY these keys: "tags", "mood", "moodScore", "moodExplanation", "takeaway".
            
            - "tags": A list of 2-4 strings representing themes.
            - "mood": A single word representing the primary emotion.
            - "moodScore": An integer from 1 to 10.
            - "moodExplanation": A one-sentence explanation of the mood.
            - "takeaway": A concise, actionable insight from the entry.
            
            Journal Entry: "$text"
            
            JSON Output:
        """.trimIndent()
    }

    private fun extractJson(response: String): String {
        // Robustness: LLMs sometimes wrap JSON in markdown blocks
        val start = response.indexOf("{")
        val end = response.lastIndexOf("}")
        if (start != -1 && end != -1 && end > start) {
            return response.substring(start, end + 1)
        }
        return response
    }

    private fun resetReleaseTimer() {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            delay(RELEASE_TIMEOUT_MS)
            withContext(Dispatchers.IO) {
                Log.d(TAG, "Inactivity timeout reached. Releasing LLM to save memory.")
                release()
            }
        }
    }

    override fun release() {
        releaseJob?.cancel()
        llmInference?.close()
        llmInference = null
        Log.d(TAG, "MediaPipe LLM released")
    }

    @Serializable
    private data class LlmAnalysisOutput(
        val tags: List<String>,
        val mood: String,
        val moodScore: Int,
        val moodExplanation: String,
        val takeaway: String
    )
}

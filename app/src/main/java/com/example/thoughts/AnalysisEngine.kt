package com.example.thoughts

/**
 * Result of the AI analysis of a journal entry.
 */
data class AnalysisResult(
    val tags: List<String>,
    val mood: String,
    val moodScore: Int, // 1-10
    val moodExplanation: String,
    val takeaway: String
)

/**
 * Abstract interface for extracting intelligence from text.
 * Part of the "Engine Pattern" to allow swapping LLMs (Gemma, Llama, etc.)
 */
interface AnalysisEngine {
    /**
     * Analyze the provided text and return structured insights.
     */
    suspend fun analyze(text: String): Result<AnalysisResult>
    
    /**
     * Initialize the engine (e.g., load model weights into RAM).
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Clean up resources.
     */
    fun release()
}

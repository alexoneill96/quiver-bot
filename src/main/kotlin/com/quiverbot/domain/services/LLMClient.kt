package com.quiverbot.domain.services

import com.quiverbot.domain.enums.SignalCategory

/**
 * Result of tweet classification by the LLM.
 */
data class ClassificationResult(
    val isSignal: Boolean,
    val signalStrength: Double,
    val category: SignalCategory,
    val tickers: List<String>,
    val summary: String
)

/**
 * Port for LLM client abstraction.
 *
 * This allows swapping between different LLM providers:
 * - OpenAI (GPT-4)
 * - Anthropic (Claude)
 * - Mock implementations for testing
 */
interface LLMClient {
    /**
     * Classify a tweet to determine if it contains a trading signal.
     *
     * @param tweetText The full text of the tweet
     * @param tweetUrl URL to the original tweet (for context)
     * @return Classification result with signal details
     */
    fun classifyTweet(tweetText: String, tweetUrl: String): ClassificationResult

    /**
     * Generate a narrative daily summary from a list of signals.
     *
     * @param signals List of signal data to summarize
     * @return Human-readable summary text
     */
    fun generateDailySummary(signals: List<SignalSummaryData>): String

    /**
     * Check if the client is properly configured.
     */
    fun healthCheck(): Boolean
}

/**
 * Data for generating daily summary.
 */
data class SignalSummaryData(
    val summary: String,
    val tickers: List<String>,
    val category: SignalCategory,
    val signalStrength: Double
)

package com.quiverbot.domain.services

/**
 * Parameters for sending a single signal alert email.
 */
data class SignalAlertEmailParams(
    val to: String,
    val recipientName: String,
    val signalSummary: String,
    val tickers: List<String>,
    val category: String,
    val signalStrength: Double,
    val tweetUrl: String,
    val tweetText: String
)

/**
 * A single signal item for batch alerts.
 */
data class BatchSignalItem(
    val signalSummary: String,
    val tickers: List<String>,
    val category: String,
    val signalStrength: Double,
    val tweetUrl: String,
    val tweetText: String
)

/**
 * Parameters for sending a batched signal alert email (multiple signals in one email).
 */
data class BatchSignalAlertEmailParams(
    val to: String,
    val recipientName: String,
    val signals: List<BatchSignalItem>
)

/**
 * Parameters for sending a daily summary email.
 */
data class DailySummaryEmailParams(
    val to: String,
    val recipientName: String,
    val summaryDate: String,
    val totalSignals: Int,
    val summaryText: String,
    val signalHighlights: List<SignalHighlight>
)

/**
 * A signal highlight for the daily summary.
 */
data class SignalHighlight(
    val summary: String,
    val tickers: List<String>,
    val category: String,
    val tweetUrl: String
)

/**
 * Port for email client abstraction.
 *
 * This allows swapping between different email providers:
 * - Sender.net
 * - Mock implementations for testing
 */
interface EmailClient {
    /**
     * Send an immediate signal alert email for a single signal.
     *
     * @return Message ID from the email provider
     */
    fun sendSignalAlert(params: SignalAlertEmailParams): String

    /**
     * Send a batched signal alert email containing multiple signals.
     *
     * @return Message ID from the email provider
     */
    fun sendBatchSignalAlert(params: BatchSignalAlertEmailParams): String

    /**
     * Send a daily summary digest email.
     *
     * @return Message ID from the email provider
     */
    fun sendDailySummary(params: DailySummaryEmailParams): String

    /**
     * Check if the client is properly configured and can send.
     */
    fun healthCheck(): Boolean
}

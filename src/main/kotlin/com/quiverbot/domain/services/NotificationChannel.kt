package com.quiverbot.domain.services

import com.quiverbot.domain.entities.Signal

/**
 * Data for alert notifications containing one or more signals.
 */
data class AlertNotificationData(
    val signals: List<SignalNotificationItem>
)

/**
 * A single signal item for notifications.
 */
data class SignalNotificationItem(
    val summary: String,
    val tickers: List<String>,
    val category: String,
    val signalStrength: Double,
    val tweetUrl: String,
    val tweetText: String
)

/**
 * Data for daily summary notifications.
 */
data class SummaryNotificationData(
    val summaryDate: String,
    val totalSignals: Int,
    val summaryText: String,
    val highlights: List<SignalHighlightItem>
)

/**
 * A signal highlight for summaries.
 */
data class SignalHighlightItem(
    val summary: String,
    val tickers: List<String>,
    val category: String,
    val tweetUrl: String
)

/**
 * Port for notification channel abstraction.
 *
 * This allows sending notifications through multiple channels:
 * - Email (Sender.net)
 * - Telegram
 * - Future channels
 *
 * Each implementation handles its own formatting and delivery.
 */
interface NotificationChannel {
    /**
     * The name of this notification channel (for logging).
     */
    val channelName: String

    /**
     * Send an alert notification for one or more signals.
     *
     * @param data The alert data containing signals
     * @return true if sent successfully, false otherwise
     */
    fun sendAlert(data: AlertNotificationData): Boolean

    /**
     * Send a daily summary notification.
     *
     * @param data The summary data
     * @return true if sent successfully, false otherwise
     */
    fun sendDailySummary(data: SummaryNotificationData): Boolean

    /**
     * Check if the channel is properly configured and can send.
     */
    fun isConfigured(): Boolean
}

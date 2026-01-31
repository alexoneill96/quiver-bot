package com.quiverbot.application.services

import com.quiverbot.domain.repositories.SignalRepository
import com.quiverbot.domain.repositories.TweetRepository
import com.quiverbot.domain.services.AlertNotificationData
import com.quiverbot.domain.services.SignalNotificationItem
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Alerting Service
 *
 * Responsible for sending immediate alerts when high-signal
 * tweets are detected. Dispatches to all enabled notification channels
 * (email, Telegram, etc). Runs frequently to minimise latency between
 * signal detection and alert delivery.
 */
@Service
class AlertingService(
    private val notificationDispatcher: NotificationDispatcher,
    private val signalRepository: SignalRepository,
    private val tweetRepository: TweetRepository,
    @Value("\${alerting.enabled:true}") private val isEnabled: Boolean,
    @Value("\${alerting.threshold:0.7}") private val alertThreshold: Double,
    @Value("\${cron.disabled:false}") private val cronDisabled: Boolean
) {
    private val isRunning = AtomicBoolean(false)

    /**
     * Scheduled job to send pending alerts.
     * Runs every 30 seconds for near-real-time alerting.
     */
    @Scheduled(cron = "\${alerting.cron:*/30 * * * * *}")
    fun sendPendingAlerts() {
        if (cronDisabled) {
            return // Silent skip when cron jobs are disabled for manual testing
        }

        if (!isEnabled) {
            logger.debug { "Alerting is disabled, skipping" }
            return
        }

        // Prevent concurrent runs
        if (!isRunning.compareAndSet(false, true)) {
            return
        }

        try {
            processPendingAlerts()
        } finally {
            isRunning.set(false)
        }
    }

    /**
     * Process all pending signal alerts.
     * Batches multiple signals into a single notification per channel.
     * Returns the number of channels that successfully sent notifications.
     */
    fun processPendingAlerts(): Int {
        // Get signals that need alerts
        val pendingSignals = signalRepository.findPendingAlerts()

        // Filter to only high-strength signals
        val alertableSignals = pendingSignals.filter { it.signalStrength >= alertThreshold }

        if (alertableSignals.isEmpty()) {
            return 0
        }

        logger.info { "Processing ${alertableSignals.size} pending alert(s)" }

        // Build signal notification items with tweet data
        val signalItems = alertableSignals.mapNotNull { signal ->
            val tweet = tweetRepository.findById(signal.tweetId)
            if (tweet == null) {
                logger.warn { "Tweet ${signal.tweetId} not found for signal ${signal.id}" }
                null
            } else {
                signal to SignalNotificationItem(
                    summary = signal.summary,
                    tickers = signal.tickers,
                    category = signal.category.name,
                    signalStrength = signal.signalStrength,
                    tweetUrl = tweet.url,
                    tweetText = tweet.text
                )
            }
        }

        if (signalItems.isEmpty()) {
            logger.warn { "No valid signal items to send" }
            return 0
        }

        // Build notification data
        val alertData = AlertNotificationData(
            signals = signalItems.map { it.second }
        )

        // Dispatch to all enabled channels
        val channelsSent = notificationDispatcher.dispatchAlert(alertData)

        // Mark all signals as sent if at least one channel succeeded
        if (channelsSent > 0) {
            signalItems.forEach { (signal, _) ->
                signalRepository.markAlertSent(signal.id)
            }
            logger.info { "Marked ${signalItems.size} signal(s) as alerted" }
        }

        logger.info { "Alert dispatched to $channelsSent channel(s) containing ${signalItems.size} signal(s)" }
        return channelsSent
    }

    /**
     * Get list of enabled notification channels.
     */
    fun getEnabledChannels(): List<String> = notificationDispatcher.getEnabledChannels()

    /**
     * Health check for notification channels.
     * Returns true if at least one channel is configured.
     */
    fun healthCheck(): Boolean = notificationDispatcher.getEnabledChannels().isNotEmpty()
}

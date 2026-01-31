package com.quiverbot.application.services

import com.quiverbot.domain.services.AlertNotificationData
import com.quiverbot.domain.services.NotificationChannel
import com.quiverbot.domain.services.SummaryNotificationData
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Notification Dispatcher Service
 *
 * Coordinates sending notifications to all enabled channels.
 * Ensures that failure in one channel does not block others.
 */
@Service
class NotificationDispatcher(
    private val channels: List<NotificationChannel>
) {

    init {
        val enabledChannels = channels.filter { it.isConfigured() }
        if (enabledChannels.isEmpty()) {
            logger.warn { "No notification channels are configured! Notifications will not be sent." }
        } else {
            logger.info { "Notification channels enabled: ${enabledChannels.joinToString { it.channelName }}" }
        }
    }

    /**
     * Dispatch an alert notification to all enabled channels.
     *
     * @param data The alert data to send
     * @return Number of channels that successfully sent the notification
     */
    fun dispatchAlert(data: AlertNotificationData): Int {
        val enabledChannels = channels.filter { it.isConfigured() }

        if (enabledChannels.isEmpty()) {
            logger.warn { "No notification channels configured - alert not sent" }
            return 0
        }

        logger.info { "Dispatching alert to ${enabledChannels.size} channel(s): ${enabledChannels.joinToString { it.channelName }}" }

        var successCount = 0

        for (channel in enabledChannels) {
            try {
                if (channel.sendAlert(data)) {
                    successCount++
                    logger.info { "Alert sent via ${channel.channelName}" }
                } else {
                    logger.warn { "Alert failed to send via ${channel.channelName}" }
                }
            } catch (e: Exception) {
                // Failure in one channel should not block others
                logger.error(e) { "Error sending alert via ${channel.channelName}" }
            }
        }

        logger.info { "Alert dispatched to $successCount/${enabledChannels.size} channels" }
        return successCount
    }

    /**
     * Dispatch a daily summary notification to all enabled channels.
     *
     * @param data The summary data to send
     * @return Number of channels that successfully sent the notification
     */
    fun dispatchSummary(data: SummaryNotificationData): Int {
        val enabledChannels = channels.filter { it.isConfigured() }

        if (enabledChannels.isEmpty()) {
            logger.warn { "No notification channels configured - summary not sent" }
            return 0
        }

        logger.info { "Dispatching summary to ${enabledChannels.size} channel(s): ${enabledChannels.joinToString { it.channelName }}" }

        var successCount = 0

        for (channel in enabledChannels) {
            try {
                if (channel.sendDailySummary(data)) {
                    successCount++
                    logger.info { "Summary sent via ${channel.channelName}" }
                } else {
                    logger.warn { "Summary failed to send via ${channel.channelName}" }
                }
            } catch (e: Exception) {
                // Failure in one channel should not block others
                logger.error(e) { "Error sending summary via ${channel.channelName}" }
            }
        }

        logger.info { "Summary dispatched to $successCount/${enabledChannels.size} channels" }
        return successCount
    }

    /**
     * Get a list of currently enabled channels.
     */
    fun getEnabledChannels(): List<String> {
        return channels.filter { it.isConfigured() }.map { it.channelName }
    }
}

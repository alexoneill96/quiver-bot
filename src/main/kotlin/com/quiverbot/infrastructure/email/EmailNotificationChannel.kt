package com.quiverbot.infrastructure.email

import com.quiverbot.domain.entities.EmailRecipient
import com.quiverbot.domain.repositories.EmailRecipientRepository
import com.quiverbot.domain.services.*
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Email notification channel adapter.
 *
 * Wraps the existing EmailClient to implement the NotificationChannel interface.
 * Sends notifications to all active email recipients.
 */
@Component
class EmailNotificationChannel(
    private val emailClient: EmailClient,
    private val recipientRepository: EmailRecipientRepository,
    @Value("\${notify.email.enabled:true}") private val enabled: Boolean
) : NotificationChannel {

    override val channelName: String = "Email"

    init {
        if (enabled) {
            logger.info { "Email notifications enabled" }
        } else {
            logger.debug { "Email notifications disabled" }
        }
    }

    override fun sendAlert(data: AlertNotificationData): Boolean {
        if (!isConfigured()) {
            logger.warn { "Email not configured, skipping alert" }
            return false
        }

        val recipients = recipientRepository.findAlertRecipients()
        if (recipients.isEmpty()) {
            logger.warn { "No email recipients configured for alerts" }
            return false
        }

        val batchParams = BatchSignalAlertEmailParams(
            to = "", // Will be set per recipient
            recipientName = "",
            signals = data.signals.map { signal ->
                BatchSignalItem(
                    signalSummary = signal.summary,
                    tickers = signal.tickers,
                    category = signal.category,
                    signalStrength = signal.signalStrength,
                    tweetUrl = signal.tweetUrl,
                    tweetText = signal.tweetText
                )
            }
        )

        var allSuccessful = true

        for (recipient in recipients) {
            try {
                val params = batchParams.copy(
                    to = recipient.email,
                    recipientName = recipient.name
                )
                emailClient.sendBatchSignalAlert(params)
                logger.info { "Alert email sent to ${recipient.email}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send alert email to ${recipient.email}" }
                allSuccessful = false
            }
        }

        return allSuccessful
    }

    override fun sendDailySummary(data: SummaryNotificationData): Boolean {
        if (!isConfigured()) {
            logger.warn { "Email not configured, skipping summary" }
            return false
        }

        val recipients = recipientRepository.findSummaryRecipients()
        if (recipients.isEmpty()) {
            logger.warn { "No email recipients configured for summaries" }
            return false
        }

        var allSuccessful = true

        for (recipient in recipients) {
            try {
                val params = DailySummaryEmailParams(
                    to = recipient.email,
                    recipientName = recipient.name,
                    summaryDate = data.summaryDate,
                    totalSignals = data.totalSignals,
                    summaryText = data.summaryText,
                    signalHighlights = data.highlights.map { highlight ->
                        SignalHighlight(
                            summary = highlight.summary,
                            tickers = highlight.tickers,
                            category = highlight.category,
                            tweetUrl = highlight.tweetUrl
                        )
                    }
                )
                emailClient.sendDailySummary(params)
                logger.info { "Summary email sent to ${recipient.email}" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to send summary email to ${recipient.email}" }
                allSuccessful = false
            }
        }

        return allSuccessful
    }

    override fun isConfigured(): Boolean {
        return enabled && emailClient.healthCheck()
    }
}

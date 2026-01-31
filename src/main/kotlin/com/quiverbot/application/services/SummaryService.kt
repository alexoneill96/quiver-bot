package com.quiverbot.application.services

import com.quiverbot.domain.entities.Signal
import com.quiverbot.domain.repositories.SignalRepository
import com.quiverbot.domain.repositories.TweetRepository
import com.quiverbot.domain.services.LLMClient
import com.quiverbot.domain.services.SignalHighlightItem
import com.quiverbot.domain.services.SignalSummaryData
import com.quiverbot.domain.services.SummaryNotificationData
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Daily Summary Service
 *
 * Responsible for generating and sending a daily digest
 * that summarizes all significant signals detected in the past 24 hours.
 * Dispatches to all enabled notification channels (email, Telegram, etc.).
 */
@Service
class SummaryService(
    private val llmClient: LLMClient,
    private val notificationDispatcher: NotificationDispatcher,
    private val signalRepository: SignalRepository,
    private val tweetRepository: TweetRepository,
    @Value("\${summary.enabled:true}") private val isEnabled: Boolean,
    @Value("\${cron.disabled:false}") private val cronDisabled: Boolean
) {
    private val isRunning = AtomicBoolean(false)

    /**
     * Scheduled job to send daily summary.
     * Default: Every day at 8 AM EST (13:00 UTC).
     */
    @Scheduled(cron = "\${summary.cron:0 0 13 * * *}")
    fun sendDailySummary() {
        if (cronDisabled) {
            return // Silent skip when cron jobs are disabled for manual testing
        }

        if (!isEnabled) {
            logger.debug { "Daily summary is disabled, skipping" }
            return
        }

        // Prevent concurrent runs
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn { "Previous summary still running, skipping" }
            return
        }

        try {
            generateAndSendSummary()
        } finally {
            isRunning.set(false)
        }
    }

    /**
     * Manually trigger daily summary generation.
     * Useful for testing or on-demand summaries.
     * Returns the number of channels that successfully sent the summary.
     */
    fun generateAndSendSummary(): Int {
        logger.info { "Generating daily summary" }

        // Get signals from the last 24 hours
        val signals = signalRepository.findLast24Hours()

        if (signals.isEmpty()) {
            logger.info { "No signals in the last 24 hours, skipping summary" }
            return 0
        }

        logger.info { "Found ${signals.size} signal(s) from last 24 hours" }

        // Generate LLM summary
        val summaryText = generateSummaryText(signals)

        // Build signal highlights (top signals by strength)
        val highlights = buildSignalHighlights(signals.take(10))

        // Format date
        val summaryDate = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")
        )

        // Build notification data
        val summaryData = SummaryNotificationData(
            summaryDate = summaryDate,
            totalSignals = signals.size,
            summaryText = summaryText,
            highlights = highlights
        )

        // Dispatch to all enabled channels
        val channelsSent = notificationDispatcher.dispatchSummary(summaryData)

        // Mark signals as included in summary if at least one channel succeeded
        if (channelsSent > 0) {
            signalRepository.markIncludedInSummary(signals.map { it.id })
            logger.info { "Marked ${signals.size} signal(s) as included in summary" }
        }

        logger.info { "Daily summary dispatched to $channelsSent channel(s)" }
        return channelsSent
    }

    /**
     * Generate a narrative summary using the LLM.
     */
    private fun generateSummaryText(signals: List<Signal>): String {
        val signalData = signals.map { s ->
            SignalSummaryData(
                summary = s.summary,
                tickers = s.tickers,
                category = s.category,
                signalStrength = s.signalStrength
            )
        }

        return llmClient.generateDailySummary(signalData)
    }

    /**
     * Build signal highlights with tweet URLs.
     */
    private fun buildSignalHighlights(signals: List<Signal>): List<SignalHighlightItem> {
        return signals.mapNotNull { signal ->
            val tweet = tweetRepository.findById(signal.tweetId)
            tweet?.let {
                SignalHighlightItem(
                    summary = signal.summary,
                    tickers = signal.tickers,
                    category = signal.category.name,
                    tweetUrl = it.url
                )
            }
        }
    }

    /**
     * Get list of enabled notification channels.
     */
    fun getEnabledChannels(): List<String> = notificationDispatcher.getEnabledChannels()
}

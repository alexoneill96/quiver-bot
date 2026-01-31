package com.quiverbot.infrastructure.telegram

import com.quiverbot.domain.services.AlertNotificationData
import com.quiverbot.domain.services.SignalNotificationItem
import com.quiverbot.domain.services.SummaryNotificationData
import kotlin.math.roundToInt

/**
 * Formats messages for Telegram notifications.
 *
 * Uses Markdown formatting for visual appeal.
 * Each signal is formatted as an individual message for clarity.
 */
object TelegramMessageFormatter {

    private const val MAX_MESSAGE_LENGTH = 4096

    /**
     * Category emoji mapping for visual appeal.
     */
    private val categoryEmojis = mapOf(
        "DIRECT_TRADE" to "💼",
        "OPTION_TRADE" to "📈",
        "INSIDER_TRADE" to "🔍",
        "LEGISLATION" to "⚖️",
        "COMMITTEE" to "🏛️",
        "LOBBYING" to "🗣️",
        "REGULATORY" to "📋",
        "OTHER" to "📊"
    )

    /**
     * Get strength indicator emoji based on signal strength.
     */
    private fun getStrengthEmoji(strength: Double): String {
        return when {
            strength >= 0.9 -> "🔥"
            strength >= 0.8 -> "💪"
            strength >= 0.7 -> "📊"
            else -> "📉"
        }
    }

    /**
     * Format a single signal for Telegram (individual message).
     */
    fun formatSingleSignal(signal: SignalNotificationItem): String {
        val categoryEmoji = categoryEmojis[signal.category] ?: "📊"
        val strengthEmoji = getStrengthEmoji(signal.signalStrength)
        val strengthPercent = (signal.signalStrength * 100).roundToInt()

        val tickerSection = if (signal.tickers.isNotEmpty()) {
            val tickersFormatted = signal.tickers.joinToString(" ") { "*\$$it*" }
            "\n🏢 Tickers: $tickersFormatted"
        } else ""

        // Truncate tweet text if too long
        val tweetPreview = if (signal.tweetText.length > 200) {
            signal.tweetText.take(197) + "..."
        } else {
            signal.tweetText
        }

        return """
🚨 *New Signal Detected!*

$categoryEmoji Category: ${signal.category.replace("_", " ")}
$strengthEmoji Strength: $strengthPercent%
$tickerSection

📝 ${signal.summary}

💬 _"${escapeMarkdown(tweetPreview)}"_

🔗 ${signal.tweetUrl}
        """.trimIndent()
    }

    /**
     * Format alert notification - returns list of individual messages (one per signal).
     */
    fun formatAlert(data: AlertNotificationData): List<String> {
        return data.signals.map { signal ->
            formatSingleSignal(signal)
        }
    }

    /**
     * Format a daily summary notification for Telegram.
     */
    fun formatSummary(data: SummaryNotificationData): List<String> {
        val header = """
📋 *QUIVERQUANT DAILY SUMMARY*
━━━━━━━━━━━━━━━━━━━━━━━━━━
📅 ${data.summaryDate}
📊 ${data.totalSignals} signal(s) detected
        """.trimIndent()

        val summarySection = """

*Summary*
${data.summaryText}
        """.trimIndent()

        val highlightsSection = if (data.highlights.isNotEmpty()) {
            val highlights = data.highlights.mapIndexed { index, signal ->
                val categoryEmoji = categoryEmojis[signal.category] ?: "📊"
                val tickerText = if (signal.tickers.isNotEmpty()) {
                    signal.tickers.joinToString(" ") { "*\$$it*" }
                } else ""

                """
${index + 1}. $categoryEmoji ${signal.summary}
   $tickerText
   🔗 ${signal.tweetUrl}
                """.trimIndent()
            }.joinToString("\n\n")

            """

📌 *Top Signals*
━━━━━━━━━━━━━━━

$highlights
            """.trimIndent()
        } else ""

        val fullMessage = "$header$summarySection$highlightsSection"

        return splitMessage(fullMessage)
    }

    /**
     * Escape special Markdown characters in user-generated content.
     */
    private fun escapeMarkdown(text: String): String {
        // In Markdown mode, we need to escape: _ * ` [
        return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("`", "\\`")
            .replace("[", "\\[")
    }

    /**
     * Split a message into chunks that fit Telegram's 4096 character limit.
     */
    private fun splitMessage(message: String): List<String> {
        if (message.length <= MAX_MESSAGE_LENGTH) {
            return listOf(message)
        }

        val messages = mutableListOf<String>()
        var remaining = message

        while (remaining.isNotEmpty()) {
            if (remaining.length <= MAX_MESSAGE_LENGTH) {
                messages.add(remaining)
                break
            }

            // Find a good break point (newline or space)
            var breakPoint = remaining.lastIndexOf('\n', MAX_MESSAGE_LENGTH)
            if (breakPoint <= 0) {
                breakPoint = remaining.lastIndexOf(' ', MAX_MESSAGE_LENGTH)
            }
            if (breakPoint <= 0) {
                breakPoint = MAX_MESSAGE_LENGTH
            }

            messages.add(remaining.substring(0, breakPoint))
            remaining = remaining.substring(breakPoint).trimStart()
        }

        return messages
    }
}

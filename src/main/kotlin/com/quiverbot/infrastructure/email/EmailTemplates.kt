package com.quiverbot.infrastructure.email

import com.quiverbot.domain.services.BatchSignalAlertEmailParams
import com.quiverbot.domain.services.SignalAlertEmailParams
import com.quiverbot.domain.services.DailySummaryEmailParams
import kotlin.math.roundToInt

/**
 * Email templates for signal alerts and daily summaries.
 */
object EmailTemplates {

    private val DISCLAIMER_TEXT = """
        This is an automated alert from QuiverQuant Signal Filter.
        Congressional trading data is publicly available but may be delayed.
        This is not financial advice. Do your own research before making any investment decisions.
        Past congressional trading patterns do not guarantee future results.
    """.trimIndent()

    private val DISCLAIMER_HTML = """
        <p style="color: #666; font-size: 12px; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
            This is an automated alert from QuiverQuant Signal Filter.<br>
            Congressional trading data is publicly available but may be delayed.<br>
            This is not financial advice. Do your own research before making any investment decisions.<br>
            Past congressional trading patterns do not guarantee future results.
        </p>
    """.trimIndent()

    // =========================================================================
    // Single Signal Alert
    // =========================================================================

    fun buildSignalAlertText(params: SignalAlertEmailParams): String {
        val tickerText = if (params.tickers.isNotEmpty()) {
            "Tickers: ${params.tickers.joinToString(", ") { "$$it" }}"
        } else ""

        return """
            QUIVERQUANT SIGNAL ALERT
            ========================

            Hi ${params.recipientName},

            A new trading signal has been detected:

            Category: ${params.category.replace("_", " ")}
            Signal Strength: ${(params.signalStrength * 100).roundToInt()}%

            ${params.signalSummary}

            $tickerText

            Original Tweet:
            "${params.tweetText}"

            View on Twitter: ${params.tweetUrl}

            ---
            $DISCLAIMER_TEXT
        """.trimIndent()
    }

    fun buildSignalAlertHtml(params: SignalAlertEmailParams): String {
        val tickerHtml = if (params.tickers.isNotEmpty()) {
            "<p><strong>Tickers:</strong> ${params.tickers.joinToString(", ") { "<span style=\"color: #2563eb;\">$$it</span>" }}</p>"
        } else ""

        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h1 style="color: #1a1a1a; border-bottom: 2px solid #2563eb; padding-bottom: 10px;">QuiverQuant Signal Alert</h1>

                <p>Hi ${params.recipientName},</p>
                <p>A new trading signal has been detected:</p>

                <div style="background: #f8fafc; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0;">
                    <p style="margin: 0 0 10px 0;"><strong>Category:</strong> ${params.category.replace("_", " ")}</p>
                    <p style="margin: 0 0 10px 0;"><strong>Signal Strength:</strong> ${(params.signalStrength * 100).roundToInt()}%</p>
                    <p style="margin: 0;">${params.signalSummary}</p>
                </div>

                $tickerHtml

                <div style="background: #f1f5f9; padding: 15px; border-radius: 8px; margin: 20px 0;">
                    <p style="margin: 0 0 10px 0; font-style: italic;">"${params.tweetText}"</p>
                    <a href="${params.tweetUrl}" style="color: #2563eb;">View on Twitter →</a>
                </div>

                $DISCLAIMER_HTML
            </body>
            </html>
        """.trimIndent()
    }

    // =========================================================================
    // Batch Signal Alert
    // =========================================================================

    fun buildBatchSignalAlertText(params: BatchSignalAlertEmailParams): String {
        val signalCount = params.signals.size
        val signalWord = if (signalCount == 1) "signal" else "signals"

        val signalsText = params.signals.mapIndexed { index, signal ->
            val tickerText = if (signal.tickers.isNotEmpty()) {
                "Tickers: ${signal.tickers.joinToString(", ") { "$$it" }}"
            } else ""

            """
                SIGNAL ${index + 1}
                --------
                Category: ${signal.category.replace("_", " ")}
                Signal Strength: ${(signal.signalStrength * 100).roundToInt()}%

                ${signal.signalSummary}

                $tickerText

                Original Tweet:
                "${signal.tweetText}"

                View on Twitter: ${signal.tweetUrl}
            """.trimIndent()
        }.joinToString("\n\n")

        return """
            QUIVERQUANT SIGNAL ALERT
            ========================
            $signalCount new $signalWord detected

            Hi ${params.recipientName},

            $signalsText

            ---
            $DISCLAIMER_TEXT
        """.trimIndent()
    }

    fun buildBatchSignalAlertHtml(params: BatchSignalAlertEmailParams): String {
        val signalCount = params.signals.size
        val signalWord = if (signalCount == 1) "signal" else "signals"

        val signalsHtml = params.signals.mapIndexed { index, signal ->
            val tickerHtml = if (signal.tickers.isNotEmpty()) {
                "<p><strong>Tickers:</strong> ${signal.tickers.joinToString(", ") { "<span style=\"color: #2563eb;\">$$it</span>" }}</p>"
            } else ""

            """
                <div style="background: #f8fafc; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0;">
                    <h3 style="margin: 0 0 10px 0; color: #1a1a1a;">Signal ${index + 1}</h3>
                    <p style="margin: 0 0 10px 0;"><strong>Category:</strong> ${signal.category.replace("_", " ")}</p>
                    <p style="margin: 0 0 10px 0;"><strong>Signal Strength:</strong> ${(signal.signalStrength * 100).roundToInt()}%</p>
                    <p style="margin: 0 0 10px 0;">${signal.signalSummary}</p>
                    $tickerHtml
                    <div style="background: #e2e8f0; padding: 10px; border-radius: 4px; margin-top: 10px;">
                        <p style="margin: 0 0 5px 0; font-style: italic; font-size: 14px;">"${signal.tweetText}"</p>
                        <a href="${signal.tweetUrl}" style="color: #2563eb; font-size: 14px;">View on Twitter →</a>
                    </div>
                </div>
            """.trimIndent()
        }.joinToString("\n")

        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h1 style="color: #1a1a1a; border-bottom: 2px solid #2563eb; padding-bottom: 10px;">QuiverQuant Signal Alert</h1>
                <p style="color: #666; font-size: 14px;">$signalCount new $signalWord detected</p>

                <p>Hi ${params.recipientName},</p>

                $signalsHtml

                $DISCLAIMER_HTML
            </body>
            </html>
        """.trimIndent()
    }

    // =========================================================================
    // Daily Summary
    // =========================================================================

    fun buildDailySummaryText(params: DailySummaryEmailParams): String {
        val highlightsText = if (params.signalHighlights.isNotEmpty()) {
            val highlights = params.signalHighlights.joinToString("\n\n") { signal ->
                val tickerText = if (signal.tickers.isNotEmpty()) {
                    "Tickers: ${signal.tickers.joinToString(", ") { "$$it" }}"
                } else ""
                """
                    [${signal.category.replace("_", " ")}] ${signal.summary}
                    $tickerText
                    Link: ${signal.tweetUrl}
                """.trimIndent()
            }
            """
                SIGNAL HIGHLIGHTS
                -----------------
                $highlights
            """.trimIndent()
        } else ""

        return """
            QUIVERQUANT DAILY SUMMARY
            =========================
            ${params.summaryDate} | ${params.totalSignals} signal(s) detected

            Hi ${params.recipientName},

            Here's your daily summary of congressional trading signals:

            SUMMARY
            -------
            ${params.summaryText}

            $highlightsText

            ---
            $DISCLAIMER_TEXT
        """.trimIndent()
    }

    fun buildDailySummaryHtml(params: DailySummaryEmailParams): String {
        val highlightsHtml = if (params.signalHighlights.isNotEmpty()) {
            val highlights = params.signalHighlights.joinToString("\n") { signal ->
                val tickerHtml = if (signal.tickers.isNotEmpty()) {
                    "<span style=\"color: #2563eb;\">${signal.tickers.joinToString(", ") { "$$it" }}</span>"
                } else ""
                """
                    <div style="background: #f8fafc; padding: 12px; border-radius: 6px; margin: 10px 0;">
                        <p style="margin: 0 0 5px 0;"><strong>[${signal.category.replace("_", " ")}]</strong> ${signal.summary}</p>
                        ${if (tickerHtml.isNotEmpty()) "<p style=\"margin: 0 0 5px 0;\">$tickerHtml</p>" else ""}
                        <a href="${signal.tweetUrl}" style="color: #2563eb; font-size: 14px;">View →</a>
                    </div>
                """.trimIndent()
            }
            """
                <h2 style="color: #1a1a1a; margin-top: 30px;">Signal Highlights</h2>
                $highlights
            """.trimIndent()
        } else ""

        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h1 style="color: #1a1a1a; border-bottom: 2px solid #2563eb; padding-bottom: 10px;">QuiverQuant Daily Summary</h1>
                <p style="color: #666; font-size: 14px;">${params.summaryDate} | ${params.totalSignals} signal(s) detected</p>

                <p>Hi ${params.recipientName},</p>
                <p>Here's your daily summary of congressional trading signals:</p>

                <div style="background: #f8fafc; border-left: 4px solid #2563eb; padding: 15px; margin: 20px 0;">
                    ${params.summaryText.replace("\n", "<br>")}
                </div>

                $highlightsHtml

                $DISCLAIMER_HTML
            </body>
            </html>
        """.trimIndent()
    }
}

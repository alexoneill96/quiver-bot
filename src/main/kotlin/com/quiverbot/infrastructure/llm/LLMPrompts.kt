package com.quiverbot.infrastructure.llm

/**
 * LLM prompts for tweet classification and summary generation.
 *
 * Prompts are versioned to allow A/B testing and gradual improvements.
 * Use ClassificationPromptVersion to select which prompt to use.
 */
object LLMPrompts {

    /**
     * Available prompt versions for classification.
     */
    enum class ClassificationPromptVersion {
        V1,  // Original broad classification
        V2   // Tighter - only actionable trading signals
    }

    /**
     * Get the classification system prompt for the specified version.
     */
    fun getClassificationSystemPrompt(version: ClassificationPromptVersion): String {
        return when (version) {
            ClassificationPromptVersion.V1 -> CLASSIFICATION_SYSTEM_PROMPT_V1
            ClassificationPromptVersion.V2 -> CLASSIFICATION_SYSTEM_PROMPT_V2
        }
    }

    // =========================================================================
    // V1 - Original broad classification (captures policy signals, trends, etc.)
    // =========================================================================

    private val CLASSIFICATION_SYSTEM_PROMPT_V1 = """
        You are a financial signal classifier analyzing tweets from QuiverQuant, a service that tracks congressional stock trading.

        Your job is to classify each tweet and extract structured information about potential trading signals.

        IMPORTANT: You must respond with ONLY valid JSON. No markdown, no explanations, just the JSON object.

        Classification Categories:
        - DIRECT_TRADE: Specific trade disclosures by named politicians (purchases, sales, options)
        - AGGREGATE_TREND: Patterns across multiple members (e.g., "senators net bought defense stocks")
        - POLICY_SIGNAL: Legislative/regulatory events that may impact specific sectors or stocks
        - LOW_SIGNAL: Promotional content, reminders, general updates, or content without actionable info

        Signal Strength Guidelines (0.0 to 1.0):
        - 0.9-1.0: Named politician + specific stock + dollar amounts + timing
        - 0.7-0.9: Named politician + specific stock OR aggregate trend with clear sector focus
        - 0.5-0.7: General trend or policy signal with some specificity
        - 0.3-0.5: Vague signals or commentary
        - 0.0-0.3: No actionable information (promotional, meta content)

        Ticker Extraction:
        - Extract any stock tickers mentioned (format: ${"$"}XXXX or just XXXX)
        - Include ETFs that represent sectors mentioned
        - If a company is named but no ticker, infer the ticker if obvious

        Summary Guidelines:
        - Write 1-2 sentences max
        - Focus on WHO, WHAT, and HOW MUCH
        - Be specific about the trading direction (buy/sell/call/put)
    """.trimIndent()

    // =========================================================================
    // V2 - Tighter classification (only actionable stock trading signals)
    // =========================================================================

    private val CLASSIFICATION_SYSTEM_PROMPT_V2 = """
        You are a financial signal classifier analyzing tweets from QuiverQuant, a service that tracks congressional stock trading.

        Your job is to identify tweets that provide ACTIONABLE TRADING SIGNALS - information that directly suggests buying or shorting a specific stock.

        IMPORTANT: You must respond with ONLY valid JSON. No markdown, no explanations, just the JSON object.

        CRITICAL: A signal is ONLY valid if it provides clear information to BUY or SHORT a specific stock.

        What IS a signal:
        - A politician bought/sold a specific stock (implies follow the trade)
        - Multiple politicians are buying/selling the same stock or sector
        - A specific company is mentioned with a clear bullish/bearish implication

        What is NOT a signal:
        - General political news (government shutdowns, policy debates) without specific stock implications
        - Promotional content, app updates, or QuiverQuant announcements
        - Vague sector commentary without specific tickers
        - News that requires too much interpretation to derive a trade

        Classification Categories:
        - DIRECT_TRADE: A named politician bought/sold a specific stock (strongest signal)
        - AGGREGATE_TREND: Multiple politicians trading same stock/sector with clear direction
        - POLICY_SIGNAL: ONLY if a specific company/ticker is clearly impacted (not general policy news)
        - LOW_SIGNAL: Everything else - promotional, news without clear trade implications, commentary

        Signal Strength Guidelines (0.0 to 1.0):
        - 0.8-1.0: Named politician + specific stock + buy/sell direction + ideally amounts
        - 0.6-0.8: Aggregate trend with specific sector/tickers and clear direction
        - 0.4-0.6: Specific stock mentioned with implied but not explicit trade direction
        - 0.0-0.4: No clear actionable trade - mark as LOW_SIGNAL with is_signal=false

        IMPORTANT: When in doubt, mark is_signal=false. We only want high-confidence trading signals.

        Ticker Extraction:
        - ONLY extract tickers that are explicitly mentioned or directly implied
        - Do NOT infer tickers from general sector/policy news
        - If no specific stock is actionable, return empty tickers array

        Summary Guidelines:
        - Write 1-2 sentences max
        - Focus on the ACTIONABLE TRADE: what to buy/sell and why
        - If not a signal, briefly explain why it's not actionable
    """.trimIndent()

    fun classificationUserPrompt(tweetText: String, tweetUrl: String): String = """
        Classify this tweet and respond with JSON only:

        Tweet: "$tweetText"
        Source: $tweetUrl

        Required JSON format:
        {
          "is_signal": boolean,
          "signal_strength": number (0.0-1.0),
          "category": "DIRECT_TRADE" | "AGGREGATE_TREND" | "POLICY_SIGNAL" | "LOW_SIGNAL",
          "tickers": ["TICKER1", "TICKER2"],
          "summary": "Brief human-readable summary"
        }
    """.trimIndent()

    val DAILY_SUMMARY_SYSTEM_PROMPT = """
        You are a financial analyst summarizing the day's congressional trading signals.

        Write a concise but comprehensive summary of the trading activity detected today.

        Guidelines:
        - Lead with the most significant/high-strength signals
        - Group related trades or trends
        - Mention specific politicians and tickers when relevant
        - Note any unusual patterns (multiple members trading same stock, sector-wide activity)
        - Keep the tone professional and factual
        - Target 2-3 paragraphs

        Do NOT include disclaimers or caveats about congressional trading - just summarize the signals.
    """.trimIndent()

    fun dailySummaryUserPrompt(signals: List<SignalData>): String {
        val signalList = signals.mapIndexed { index, s ->
            """
            Signal ${index + 1} (${s.category}, strength: ${s.signalStrength}):
            - Summary: ${s.summary}
            - Tickers: ${s.tickers.joinToString(", ").ifEmpty { "None" }}
            """.trimIndent()
        }.joinToString("\n\n")

        return """
            Summarize these ${signals.size} trading signals detected today:

            $signalList

            Write a 2-3 paragraph summary of today's notable congressional trading activity.
        """.trimIndent()
    }

    data class SignalData(
        val summary: String,
        val tickers: List<String>,
        val category: String,
        val signalStrength: Double
    )
}

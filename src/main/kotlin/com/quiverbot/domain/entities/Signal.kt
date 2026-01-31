package com.quiverbot.domain.entities

import com.quiverbot.domain.enums.SignalCategory
import java.time.Instant
import java.util.UUID

/**
 * Domain entity representing a classified signal derived from a tweet.
 * Contains the LLM classification results and metadata.
 */
data class Signal(
    /** Unique identifier for the signal */
    val id: UUID,

    /** Reference to the source tweet */
    val tweetId: String,

    /** Whether this was classified as a true trading signal */
    val isSignal: Boolean,

    /** Confidence score from 0.0 to 1.0 */
    val signalStrength: Double,

    /** Classification category */
    val category: SignalCategory,

    /** Extracted stock ticker symbols */
    val tickers: List<String>,

    /** LLM-generated summary of the signal */
    val summary: String,

    /** When this classification was made */
    val classifiedAt: Instant,

    /** Whether an alert email has been sent for this signal */
    val alertSent: Boolean,

    /** Whether this signal was included in a daily summary */
    val includedInSummary: Boolean
)

/**
 * Data required to create a new Signal entity.
 */
data class CreateSignalData(
    val tweetId: String,
    val isSignal: Boolean,
    val signalStrength: Double,
    val category: SignalCategory,
    val tickers: List<String>,
    val summary: String
)

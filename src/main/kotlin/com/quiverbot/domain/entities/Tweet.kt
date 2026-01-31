package com.quiverbot.domain.entities

import java.time.Instant

/**
 * Domain entity representing a raw tweet from QuiverQuant.
 * This is the pure domain model, independent of persistence layer.
 */
data class Tweet(
    /** Unique identifier (from Twitter) */
    val id: String,

    /** Full text content of the tweet */
    val text: String,

    /** Original author username (should always be QuiverQuant) */
    val authorUsername: String,

    /** When the tweet was posted on Twitter */
    val postedAt: Instant,

    /** When we ingested this tweet */
    val ingestedAt: Instant,

    /** URL to the original tweet */
    val url: String,

    /** Whether this tweet has been processed by the classifier */
    val isProcessed: Boolean
)

/**
 * Data required to create a new Tweet entity.
 */
data class CreateTweetData(
    val id: String,
    val text: String,
    val authorUsername: String,
    val postedAt: Instant,
    val url: String
)

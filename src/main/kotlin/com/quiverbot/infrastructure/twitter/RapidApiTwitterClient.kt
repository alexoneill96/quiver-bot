package com.quiverbot.infrastructure.twitter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.quiverbot.domain.entities.CreateTweetData
import com.quiverbot.domain.services.TwitterClient
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val logger = KotlinLogging.logger {}

/**
 * RapidAPI Twitter client implementation.
 *
 * Uses a RapidAPI Twitter endpoint to fetch tweets from the QuiverQuant account.
 * This is an alternative to the official Twitter API v2, useful when:
 * - Official API access is restricted or expensive
 * - You need a simpler authentication model (API key vs OAuth)
 *
 * RATE LIMIT CONSTRAINTS (CRITICAL):
 * - Free tier allows only 1,000 requests per month
 * - To conserve quota, this client:
 *   1. Fetches at most ONE page per poll (no deep pagination)
 *   2. Stops processing once an already-seen tweet_id is encountered
 *   3. Does NOT perform username -> ID lookups (uses pre-configured rest_id)
 */
@Component
class RapidApiTwitterClient(
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    @Value("\${rapidapi.key:}") private val apiKey: String,
    @Value("\${quiverquant.rest-id:}") private val restId: String,
    @Value("\${twitter.target-username:QuiverQuant}") private val targetUsername: String
) : TwitterClient {

    private val baseUrl = "https://twitter-api45.p.rapidapi.com/timeline.php"

    init {
        if (apiKey.isBlank()) {
            logger.warn { "RAPIDAPI_KEY not set - RapidAPI Twitter client will fail" }
        }
        if (restId.isBlank()) {
            logger.warn { "QUIVERQUANT_REST_ID not set - RapidAPI Twitter client will fail" }
        }
    }

    /**
     * Fetch recent tweets from the QuiverQuant account.
     *
     * RATE LIMIT STRATEGY:
     * - Only fetches ONE page per call to conserve the 1,000 requests/month quota
     * - Stops early if a tweet matching sinceId is encountered
     * - Does NOT paginate through historical tweets
     */
    override fun fetchUserTweets(
        username: String,
        sinceId: String?,
        limit: Int
    ): List<CreateTweetData> {
        if (apiKey.isBlank() || restId.isBlank()) {
            logger.error { "RapidAPI client not configured - missing RAPIDAPI_KEY or QUIVERQUANT_REST_ID" }
            return emptyList()
        }

        try {
            val url = "$baseUrl?rest_id=$restId"
            logger.debug { "Fetching tweets from RapidAPI for rest_id: $restId" }

            val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("x-rapidapi-key", apiKey)
                .addHeader("x-rapidapi-host", "twitter-api45.p.rapidapi.com")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No body"
                    logger.error { "RapidAPI request failed: ${response.code} - $errorBody" }
                    return emptyList()
                }

                val body = response.body?.string() ?: return emptyList()
                val data = objectMapper.readValue(body, RapidApiTimelineResponse::class.java)

                if (data.timeline.isNullOrEmpty()) {
                    logger.debug { "No timeline data in RapidAPI response" }
                    return emptyList()
                }

                val tweets = mutableListOf<CreateTweetData>()

                for (tweet in data.timeline) {
                    // Stop when we hit a tweet we've already seen
                    if (sinceId != null && tweet.tweet_id == sinceId) {
                        logger.debug { "Encountered already-seen tweet $sinceId, stopping" }
                        break
                    }

                    // Also stop if we've somehow fetched a tweet older than sinceId
                    if (sinceId != null) {
                        try {
                            if (tweet.tweet_id.toBigInteger() <= sinceId.toBigInteger()) {
                                logger.debug { "Tweet ${tweet.tweet_id} is older than sinceId $sinceId, stopping" }
                                break
                            }
                        } catch (e: NumberFormatException) {
                            // If comparison fails, continue processing
                        }
                    }

                    tweets.add(
                        CreateTweetData(
                            id = tweet.tweet_id,
                            text = tweet.text,
                            authorUsername = username,
                            postedAt = parseCreatedAt(tweet.created_at),
                            url = "https://twitter.com/$username/status/${tweet.tweet_id}"
                        )
                    )

                    if (tweets.size >= limit) {
                        break
                    }
                }

                logger.info { "Fetched ${tweets.size} new tweets from RapidAPI" }
                return tweets
            }
        } catch (e: Exception) {
            logger.error(e) { "RapidAPI request failed" }
            return emptyList()
        }
    }

    /**
     * Check if the RapidAPI client is properly configured.
     */
    override fun healthCheck(): Boolean {
        if (apiKey.isBlank()) {
            logger.debug { "Health check failed: RAPIDAPI_KEY not set" }
            return false
        }

        if (restId.isBlank()) {
            logger.debug { "Health check failed: QUIVERQUANT_REST_ID not set" }
            return false
        }

        return true
    }

    /**
     * Parse the created_at timestamp from RapidAPI response.
     */
    private fun parseCreatedAt(createdAt: String): Instant {
        // Try parsing as ISO 8601 first
        try {
            return Instant.parse(createdAt)
        } catch (e: DateTimeParseException) {
            // Continue to next format
        }

        // Try Twitter's standard format: "Wed Oct 10 20:19:24 +0000 2018"
        try {
            val formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss Z yyyy")
            return formatter.parse(createdAt, Instant::from)
        } catch (e: DateTimeParseException) {
            // Continue to next format
        }

        // Fallback to current time if parsing fails
        logger.warn { "Could not parse created_at \"$createdAt\", using current time" }
        return Instant.now()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class RapidApiTimelineResponse(
    val timeline: List<RapidApiTweet>? = null,
    val next_cursor: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class RapidApiTweet(
    val tweet_id: String = "",
    val created_at: String = "",
    val text: String = ""
)

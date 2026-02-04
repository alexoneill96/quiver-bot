package com.quiverbot.application.services

import com.quiverbot.domain.repositories.TweetRepository
import com.quiverbot.domain.services.TwitterClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Tweet Ingestion Service
 *
 * Responsible for polling the QuiverQuant Twitter account at regular intervals
 * and storing new tweets in the database for subsequent classification.
 */
@Service
class IngestionService(
    private val twitterClient: TwitterClient,
    private val tweetRepository: TweetRepository,
    @Value("\${twitter.target-username:QuiverQuant}") private val targetUsername: String,
    @Value("\${ingestion.enabled:true}") private val isEnabled: Boolean,
    @Value("\${cron.disabled:false}") private val cronDisabled: Boolean
) {
    private val isRunning = AtomicBoolean(false)

    /**
     * Scheduled job to poll for new tweets.
     * Runs every 5 minutes by default.
     */
    @Scheduled(cron = "\${ingestion.cron:0 */45 * * * *}")
    fun pollForNewTweets() {
        if (cronDisabled) {
            return // Silent skip when cron jobs are disabled for manual testing
        }

        if (!isEnabled) {
            logger.debug { "Ingestion is disabled, skipping poll" }
            return
        }

        // Prevent concurrent runs
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn { "Previous poll still running, skipping" }
            return
        }

        try {
            ingestNewTweets()
        } finally {
            isRunning.set(false)
        }
    }

    /**
     * Manually trigger tweet ingestion.
     * Useful for testing or on-demand refreshes.
     */
    fun ingestNewTweets(): Int {
        logger.info { "Starting tweet ingestion for @$targetUsername" }

        try {
            // Get the most recent tweet ID we have
            val sinceId = tweetRepository.getLatestTweetId()
            logger.debug { "Fetching tweets since ID: ${sinceId ?: "beginning"}" }

            // Fetch new tweets from Twitter
            val tweets = twitterClient.fetchUserTweets(
                username = targetUsername,
                sinceId = sinceId,
                limit = 100
            )

            if (tweets.isEmpty()) {
                logger.debug { "No new tweets found" }
                return 0
            }

            // Save tweets to database (duplicates are handled by the repository)
            val savedCount = tweetRepository.saveBatch(tweets)

            logger.info { "Ingested $savedCount new tweet(s) out of ${tweets.size} fetched" }
            return savedCount
        } catch (e: Exception) {
            logger.error(e) { "Tweet ingestion failed" }
            throw e
        }
    }

    /**
     * Check if the Twitter client is healthy and can fetch tweets.
     */
    fun healthCheck(): Boolean = twitterClient.healthCheck()
}

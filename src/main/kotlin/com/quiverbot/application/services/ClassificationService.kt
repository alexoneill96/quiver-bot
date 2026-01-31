package com.quiverbot.application.services

import com.quiverbot.domain.entities.CreateSignalData
import com.quiverbot.domain.entities.Tweet
import com.quiverbot.domain.repositories.SignalRepository
import com.quiverbot.domain.repositories.TweetRepository
import com.quiverbot.domain.services.LLMClient
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Classification Service
 *
 * Responsible for processing unclassified tweets through the LLM
 * and storing the resulting signals in the database.
 */
@Service
class ClassificationService(
    private val llmClient: LLMClient,
    private val tweetRepository: TweetRepository,
    private val signalRepository: SignalRepository,
    @Value("\${classification.signal-threshold:0.5}") private val signalThreshold: Double,
    @Value("\${classification.batch-size:10}") private val batchSize: Int,
    @Value("\${classification.enabled:true}") private val isEnabled: Boolean,
    @Value("\${cron.disabled:false}") private val cronDisabled: Boolean
) {
    private val isRunning = AtomicBoolean(false)

    /**
     * Scheduled job to classify unprocessed tweets.
     * Runs every minute to process tweets shortly after ingestion.
     */
    @Scheduled(cron = "\${classification.cron:0 * * * * *}")
    fun classifyNewTweets() {
        if (cronDisabled) {
            return // Silent skip when cron jobs are disabled for manual testing
        }

        if (!isEnabled) {
            logger.debug { "Classification is disabled, skipping" }
            return
        }

        // Prevent concurrent runs
        if (!isRunning.compareAndSet(false, true)) {
            logger.debug { "Previous classification still running, skipping" }
            return
        }

        try {
            processUnclassifiedTweets()
        } finally {
            isRunning.set(false)
        }
    }

    /**
     * Process all unclassified tweets through the LLM.
     * Returns the number of signals created (not total tweets processed).
     */
    fun processUnclassifiedTweets(): Int {
        val tweets = tweetRepository.findUnprocessed(batchSize)

        if (tweets.isEmpty()) {
            logger.debug { "No unprocessed tweets found" }
            return 0
        }

        logger.info { "Processing ${tweets.size} unclassified tweet(s)" }

        var signalsCreated = 0

        // Process tweets sequentially to avoid overwhelming the LLM API
        for (tweet in tweets) {
            try {
                val wasSignal = classifyTweet(tweet)
                if (wasSignal) signalsCreated++
            } catch (e: Exception) {
                logger.error(e) { "Failed to classify tweet ${tweet.id}" }
                // Continue with other tweets even if one fails
            }
        }

        logger.info { "Classified ${tweets.size} tweet(s), created $signalsCreated signal(s)" }
        return signalsCreated
    }

    /**
     * Classify a single tweet and store the result.
     * Returns true if the tweet was classified as a signal above threshold.
     */
    private fun classifyTweet(tweet: Tweet): Boolean {
        logger.debug { "Classifying tweet ${tweet.id}" }

        // Get classification from LLM
        val result = llmClient.classifyTweet(tweet.text, tweet.url)

        // Determine if this meets our signal threshold
        val isAboveThreshold = result.signalStrength >= signalThreshold

        // Store the signal (we store all classifications for auditing)
        signalRepository.save(
            CreateSignalData(
                tweetId = tweet.id,
                isSignal = result.isSignal && isAboveThreshold,
                signalStrength = result.signalStrength,
                category = result.category,
                tickers = result.tickers,
                summary = result.summary
            )
        )

        // Mark tweet as processed
        tweetRepository.markAsProcessed(tweet.id)

        if (result.isSignal && isAboveThreshold) {
            logger.info { "Signal detected: [${result.category}] ${result.summary.take(50)}... (strength: ${result.signalStrength})" }
            return true
        }

        logger.debug { "Tweet ${tweet.id} classified as low signal (strength: ${result.signalStrength})" }
        return false
    }

    /**
     * Check if the LLM client is healthy.
     */
    fun healthCheck(): Boolean = llmClient.healthCheck()
}

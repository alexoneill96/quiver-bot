package com.quiverbot.domain.repositories

import com.quiverbot.domain.entities.CreateTweetData
import com.quiverbot.domain.entities.Tweet

/**
 * Repository interface for Tweet persistence.
 * Implementation is in the infrastructure layer.
 */
interface TweetRepository {
    /**
     * Save a single tweet. Returns null if tweet already exists.
     */
    fun save(data: CreateTweetData): Tweet?

    /**
     * Save multiple tweets, ignoring duplicates.
     * Returns the count of actually inserted tweets.
     */
    fun saveBatch(data: List<CreateTweetData>): Int

    /**
     * Find a tweet by its ID.
     */
    fun findById(id: String): Tweet?

    /**
     * Find tweets that haven't been processed yet.
     */
    fun findUnprocessed(limit: Int = 100): List<Tweet>

    /**
     * Mark a tweet as processed.
     */
    fun markAsProcessed(id: String)

    /**
     * Get the ID of the most recently posted tweet.
     */
    fun getLatestTweetId(): String?

    /**
     * Delete all tweets.
     */
    fun deleteAll(): Int
}

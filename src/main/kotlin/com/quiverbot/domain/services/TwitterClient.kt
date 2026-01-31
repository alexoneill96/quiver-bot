package com.quiverbot.domain.services

import com.quiverbot.domain.entities.CreateTweetData

/**
 * Port for Twitter/X client abstraction.
 *
 * This allows swapping between different Twitter access methods:
 * - Official Twitter API v2
 * - RapidAPI Twitter
 * - Mock implementations for testing
 */
interface TwitterClient {
    /**
     * Fetch recent tweets from a specific user.
     *
     * @param username Twitter username (without @)
     * @param sinceId Only fetch tweets newer than this ID
     * @param limit Maximum number of tweets to fetch
     * @return List of tweet data ready for persistence
     */
    fun fetchUserTweets(
        username: String,
        sinceId: String? = null,
        limit: Int = 100
    ): List<CreateTweetData>

    /**
     * Check if the client is properly configured and can connect.
     */
    fun healthCheck(): Boolean
}

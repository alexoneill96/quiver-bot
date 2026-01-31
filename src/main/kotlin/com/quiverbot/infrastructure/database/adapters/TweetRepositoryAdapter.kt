package com.quiverbot.infrastructure.database.adapters

import com.quiverbot.domain.entities.CreateTweetData
import com.quiverbot.domain.entities.Tweet
import com.quiverbot.domain.repositories.TweetRepository
import com.quiverbot.infrastructure.database.entities.TweetEntity
import com.quiverbot.infrastructure.database.jpa.JpaTweetRepository
import mu.KotlinLogging
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * Repository adapter that implements the domain TweetRepository
 * using Spring Data JPA.
 */
@Component
class TweetRepositoryAdapter(
    private val jpaRepository: JpaTweetRepository
) : TweetRepository {

    override fun save(data: CreateTweetData): Tweet? {
        // Check if tweet already exists
        if (jpaRepository.existsById(data.id)) {
            logger.debug { "Tweet ${data.id} already exists, skipping" }
            return null
        }

        val entity = TweetEntity(
            id = data.id,
            text = data.text,
            authorUsername = data.authorUsername,
            postedAt = data.postedAt,
            ingestedAt = Instant.now(),
            url = data.url,
            isProcessed = false
        )

        return jpaRepository.save(entity).toDomain()
    }

    @Transactional
    override fun saveBatch(data: List<CreateTweetData>): Int {
        var savedCount = 0
        for (tweet in data) {
            if (save(tweet) != null) {
                savedCount++
            }
        }
        return savedCount
    }

    override fun findById(id: String): Tweet? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findUnprocessed(limit: Int): List<Tweet> {
        return jpaRepository
            .findByIsProcessedFalseOrderByPostedAtAsc(PageRequest.of(0, limit))
            .map { it.toDomain() }
    }

    @Transactional
    override fun markAsProcessed(id: String) {
        jpaRepository.markAsProcessed(id)
    }

    override fun getLatestTweetId(): String? {
        return jpaRepository.findFirstByOrderByPostedAtDesc()?.id
    }
}

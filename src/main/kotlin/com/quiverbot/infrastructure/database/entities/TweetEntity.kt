package com.quiverbot.infrastructure.database.entities

import com.quiverbot.domain.entities.Tweet
import jakarta.persistence.*
import java.time.Instant

/**
 * JPA entity for Tweet persistence.
 */
@Entity
@Table(name = "tweets")
class TweetEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: String = "",

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    var text: String = "",

    @Column(name = "author_username", nullable = false)
    var authorUsername: String = "",

    @Column(name = "posted_at", nullable = false)
    var postedAt: Instant = Instant.now(),

    @Column(name = "ingested_at", nullable = false)
    var ingestedAt: Instant = Instant.now(),

    @Column(name = "url", nullable = false)
    var url: String = "",

    @Column(name = "is_processed", nullable = false)
    var isProcessed: Boolean = false
) {
    /**
     * Convert JPA entity to domain model.
     */
    fun toDomain(): Tweet = Tweet(
        id = id,
        text = text,
        authorUsername = authorUsername,
        postedAt = postedAt,
        ingestedAt = ingestedAt,
        url = url,
        isProcessed = isProcessed
    )
}

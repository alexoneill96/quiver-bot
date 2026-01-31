package com.quiverbot.infrastructure.database.jpa

import com.quiverbot.infrastructure.database.entities.TweetEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for TweetEntity.
 */
@Repository
interface JpaTweetRepository : JpaRepository<TweetEntity, String> {

    /**
     * Find tweets that haven't been processed yet.
     */
    fun findByIsProcessedFalseOrderByPostedAtAsc(pageable: Pageable): List<TweetEntity>

    /**
     * Find the most recent tweet by posted date.
     */
    fun findFirstByOrderByPostedAtDesc(): TweetEntity?

    /**
     * Mark a tweet as processed.
     */
    @Modifying
    @Query("UPDATE TweetEntity t SET t.isProcessed = true WHERE t.id = :id")
    fun markAsProcessed(id: String)

    // Note: existsById is inherited from JpaRepository
}

package com.quiverbot.infrastructure.database.jpa

import com.quiverbot.infrastructure.database.entities.EmailRecipientEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Spring Data JPA repository for EmailRecipientEntity.
 */
@Repository
interface JpaEmailRecipientRepository : JpaRepository<EmailRecipientEntity, UUID> {

    /**
     * Find all active recipients who want to receive alerts.
     */
    fun findByIsActiveTrueAndReceivesAlertsTrue(): List<EmailRecipientEntity>

    /**
     * Find all active recipients who want to receive daily summaries.
     */
    fun findByIsActiveTrueAndReceivesSummaryTrue(): List<EmailRecipientEntity>

    /**
     * Find a recipient by email address.
     */
    fun findByEmail(email: String): EmailRecipientEntity?

    /**
     * Delete a recipient by email address.
     */
    fun deleteByEmail(email: String): Int
}

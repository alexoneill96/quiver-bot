package com.quiverbot.infrastructure.database.adapters

import com.quiverbot.domain.entities.EmailRecipient
import com.quiverbot.domain.repositories.EmailRecipientRepository
import com.quiverbot.infrastructure.database.entities.EmailRecipientEntity
import com.quiverbot.infrastructure.database.jpa.JpaEmailRecipientRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Repository adapter that implements the domain EmailRecipientRepository
 * using Spring Data JPA.
 */
@Component
class EmailRecipientRepositoryAdapter(
    private val jpaRepository: JpaEmailRecipientRepository
) : EmailRecipientRepository {

    override fun findAlertRecipients(): List<EmailRecipient> {
        return jpaRepository
            .findByIsActiveTrueAndReceivesAlertsTrue()
            .map { it.toDomain() }
    }

    override fun findSummaryRecipients(): List<EmailRecipient> {
        return jpaRepository
            .findByIsActiveTrueAndReceivesSummaryTrue()
            .map { it.toDomain() }
    }

    override fun findAll(): List<EmailRecipient> {
        return jpaRepository.findAll().map { it.toDomain() }
    }

    override fun findByEmail(email: String): EmailRecipient? {
        return jpaRepository.findByEmail(email)?.toDomain()
    }

    override fun save(
        email: String,
        name: String,
        receivesAlerts: Boolean,
        receivesSummary: Boolean
    ): EmailRecipient {
        // Check if recipient already exists
        val existing = jpaRepository.findByEmail(email)

        val entity = if (existing != null) {
            // Update existing
            existing.apply {
                this.name = name
                this.receivesAlerts = receivesAlerts
                this.receivesSummary = receivesSummary
                this.isActive = true
            }
        } else {
            // Create new
            EmailRecipientEntity(
                email = email,
                name = name,
                receivesAlerts = receivesAlerts,
                receivesSummary = receivesSummary
            )
        }

        return jpaRepository.save(entity).toDomain()
    }

    @Transactional
    override fun deleteByEmail(email: String): Boolean {
        return jpaRepository.deleteByEmail(email) > 0
    }
}

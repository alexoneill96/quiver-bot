package com.quiverbot.domain.repositories

import com.quiverbot.domain.entities.EmailRecipient

/**
 * Repository interface for EmailRecipient persistence.
 * Implementation is in the infrastructure layer.
 */
interface EmailRecipientRepository {
    /**
     * Find all active recipients who want to receive alerts.
     */
    fun findAlertRecipients(): List<EmailRecipient>

    /**
     * Find all active recipients who want to receive daily summaries.
     */
    fun findSummaryRecipients(): List<EmailRecipient>

    /**
     * Find all recipients.
     */
    fun findAll(): List<EmailRecipient>

    /**
     * Find a recipient by email address.
     */
    fun findByEmail(email: String): EmailRecipient?

    /**
     * Save a new recipient or update existing one.
     */
    fun save(
        email: String,
        name: String,
        receivesAlerts: Boolean = true,
        receivesSummary: Boolean = true
    ): EmailRecipient

    /**
     * Delete a recipient by email.
     */
    fun deleteByEmail(email: String): Boolean
}

package com.quiverbot.domain.entities

import java.time.Instant
import java.util.UUID

/**
 * Domain entity representing an email recipient for alerts and summaries.
 */
data class EmailRecipient(
    /** Unique identifier */
    val id: UUID,

    /** Email address */
    val email: String,

    /** Display name */
    val name: String,

    /** Whether this recipient receives real-time alerts */
    val receivesAlerts: Boolean,

    /** Whether this recipient receives daily summaries */
    val receivesSummary: Boolean,

    /** Whether this recipient is active */
    val isActive: Boolean,

    /** When this recipient was created */
    val createdAt: Instant
)

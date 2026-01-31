package com.quiverbot.domain.repositories

import com.quiverbot.domain.entities.CreateSignalData
import com.quiverbot.domain.entities.Signal
import java.util.UUID

/**
 * Repository interface for Signal persistence.
 * Implementation is in the infrastructure layer.
 */
interface SignalRepository {
    /**
     * Save a new signal classification.
     */
    fun save(data: CreateSignalData): Signal

    /**
     * Find a signal by its ID.
     */
    fun findById(id: UUID): Signal?

    /**
     * Find signals that are true signals but haven't had alerts sent.
     */
    fun findPendingAlerts(): List<Signal>

    /**
     * Mark a signal as having had its alert sent.
     */
    fun markAlertSent(id: UUID)

    /**
     * Find signals from the last 24 hours that are true signals.
     */
    fun findLast24Hours(): List<Signal>

    /**
     * Mark signals as included in a daily summary.
     */
    fun markIncludedInSummary(ids: List<UUID>)

    /**
     * Find all signals, ordered by classification time descending.
     */
    fun findAll(): List<Signal>

    /**
     * Delete all signals.
     */
    fun deleteAll(): Int
}

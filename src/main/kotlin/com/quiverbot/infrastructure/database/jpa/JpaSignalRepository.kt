package com.quiverbot.infrastructure.database.jpa

import com.quiverbot.infrastructure.database.entities.SignalEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

/**
 * Spring Data JPA repository for SignalEntity.
 */
@Repository
interface JpaSignalRepository : JpaRepository<SignalEntity, UUID> {

    /**
     * Find signals that are true signals but haven't had alerts sent.
     */
    fun findByIsSignalTrueAndAlertSentFalseOrderByClassifiedAtAsc(): List<SignalEntity>

    /**
     * Find signals from a given time onwards that are true signals.
     */
    fun findByIsSignalTrueAndClassifiedAtGreaterThanEqualOrderBySignalStrengthDesc(
        since: Instant
    ): List<SignalEntity>

    /**
     * Mark a signal as having had its alert sent.
     */
    @Modifying
    @Query("UPDATE SignalEntity s SET s.alertSent = true WHERE s.id = :id")
    fun markAlertSent(id: UUID)

    /**
     * Mark multiple signals as included in a summary.
     */
    @Modifying
    @Query("UPDATE SignalEntity s SET s.includedInSummary = true WHERE s.id IN :ids")
    fun markIncludedInSummary(ids: List<UUID>)
}

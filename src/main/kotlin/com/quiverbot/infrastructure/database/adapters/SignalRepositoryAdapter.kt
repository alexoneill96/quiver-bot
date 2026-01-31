package com.quiverbot.infrastructure.database.adapters

import com.quiverbot.domain.entities.CreateSignalData
import com.quiverbot.domain.entities.Signal
import com.quiverbot.domain.repositories.SignalRepository
import com.quiverbot.infrastructure.database.entities.SignalEntity
import com.quiverbot.infrastructure.database.jpa.JpaSignalRepository
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * Repository adapter that implements the domain SignalRepository
 * using Spring Data JPA.
 */
@Component
class SignalRepositoryAdapter(
    private val jpaRepository: JpaSignalRepository
) : SignalRepository {

    override fun save(data: CreateSignalData): Signal {
        val entity = SignalEntity(
            tweetId = data.tweetId,
            isSignal = data.isSignal,
            signalStrength = data.signalStrength,
            category = data.category,
            tickers = data.tickers,
            summary = data.summary,
            classifiedAt = Instant.now(),
            alertSent = false,
            includedInSummary = false
        )

        return jpaRepository.save(entity).toDomain()
    }

    override fun findById(id: UUID): Signal? {
        return jpaRepository.findById(id).orElse(null)?.toDomain()
    }

    override fun findPendingAlerts(): List<Signal> {
        return jpaRepository
            .findByIsSignalTrueAndAlertSentFalseOrderByClassifiedAtAsc()
            .map { it.toDomain() }
    }

    @Transactional
    override fun markAlertSent(id: UUID) {
        jpaRepository.markAlertSent(id)
    }

    override fun findLast24Hours(): List<Signal> {
        val since = Instant.now().minus(24, ChronoUnit.HOURS)
        return jpaRepository
            .findByIsSignalTrueAndClassifiedAtGreaterThanEqualOrderBySignalStrengthDesc(since)
            .map { it.toDomain() }
    }

    @Transactional
    override fun markIncludedInSummary(ids: List<UUID>) {
        if (ids.isNotEmpty()) {
            jpaRepository.markIncludedInSummary(ids)
        }
    }

    override fun findAll(): List<Signal> {
        return jpaRepository.findAll()
            .sortedByDescending { it.classifiedAt }
            .map { it.toDomain() }
    }
}

package com.quiverbot.infrastructure.database.entities

import com.quiverbot.domain.entities.Signal
import com.quiverbot.domain.enums.SignalCategory
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for Signal persistence.
 */
@Entity
@Table(name = "signals")
class SignalEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null,

    @Column(name = "tweet_id", nullable = false)
    var tweetId: String = "",

    @Column(name = "is_signal", nullable = false)
    var isSignal: Boolean = false,

    @Column(name = "signal_strength", nullable = false)
    var signalStrength: Double = 0.0,

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    var category: SignalCategory = SignalCategory.LOW_SIGNAL,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "signal_tickers", joinColumns = [JoinColumn(name = "signal_id")])
    @Column(name = "ticker")
    var tickers: List<String> = emptyList(),

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    var summary: String = "",

    @Column(name = "classified_at", nullable = false)
    var classifiedAt: Instant = Instant.now(),

    @Column(name = "alert_sent", nullable = false)
    var alertSent: Boolean = false,

    @Column(name = "included_in_summary", nullable = false)
    var includedInSummary: Boolean = false
) {
    /**
     * Convert JPA entity to domain model.
     */
    fun toDomain(): Signal = Signal(
        id = id ?: throw IllegalStateException("Signal ID is null"),
        tweetId = tweetId,
        isSignal = isSignal,
        signalStrength = signalStrength,
        category = category,
        tickers = tickers.toList(),
        summary = summary,
        classifiedAt = classifiedAt,
        alertSent = alertSent,
        includedInSummary = includedInSummary
    )
}

package com.quiverbot.infrastructure.database.entities

import com.quiverbot.domain.entities.EmailRecipient
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * JPA entity for EmailRecipient persistence.
 */
@Entity
@Table(name = "email_recipients")
class EmailRecipientEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    var id: UUID? = null,

    @Column(name = "email", nullable = false, unique = true)
    var email: String = "",

    @Column(name = "name", nullable = false)
    var name: String = "",

    @Column(name = "receives_alerts", nullable = false)
    var receivesAlerts: Boolean = true,

    @Column(name = "receives_summary", nullable = false)
    var receivesSummary: Boolean = true,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
) {
    /**
     * Convert JPA entity to domain model.
     */
    fun toDomain(): EmailRecipient = EmailRecipient(
        id = id ?: throw IllegalStateException("EmailRecipient ID is null"),
        email = email,
        name = name,
        receivesAlerts = receivesAlerts,
        receivesSummary = receivesSummary,
        isActive = isActive,
        createdAt = createdAt
    )
}

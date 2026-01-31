package com.quiverbot.application.controllers

import com.quiverbot.application.services.AlertingService
import com.quiverbot.application.services.ClassificationService
import com.quiverbot.application.services.IngestionService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Health check controller for monitoring application status.
 */
@RestController
@RequestMapping("/health")
class HealthController(
    private val ingestionService: IngestionService,
    private val classificationService: ClassificationService,
    private val alertingService: AlertingService
) {

    @GetMapping
    fun health(): HealthResponse {
        val twitterHealth = runCatching { ingestionService.healthCheck() }.getOrDefault(false)
        val llmHealth = runCatching { classificationService.healthCheck() }.getOrDefault(false)
        val notificationsHealth = runCatching { alertingService.healthCheck() }.getOrDefault(false)

        val allHealthy = twitterHealth && llmHealth && notificationsHealth

        return HealthResponse(
            status = if (allHealthy) "healthy" else "degraded",
            services = ServiceHealth(
                twitter = twitterHealth,
                llm = llmHealth,
                notifications = notificationsHealth
            )
        )
    }
}

data class HealthResponse(
    val status: String,
    val services: ServiceHealth
)

data class ServiceHealth(
    val twitter: Boolean,
    val llm: Boolean,
    val notifications: Boolean
)

package com.quiverbot

import mu.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

private val logger = KotlinLogging.logger {}

/**
 * QuiverQuant Signal Filter & Alerting Service
 *
 * A production-ready backend service that monitors the QuiverQuant Twitter account
 * for congressional trading signals, classifies them using an LLM, and sends
 * real-time alerts plus daily summary digests.
 */
@SpringBootApplication
@EnableScheduling
class QuiverBotApplication

fun main(args: Array<String>) {
    val context = runApplication<QuiverBotApplication>(*args)

    val port = context.environment.getProperty("server.port", "3000")
    logger.info { "QuiverQuant Signal Service running on port $port" }
    logger.info { "Health check: http://localhost:$port/health" }
    logger.info { "Readiness check: http://localhost:$port/health/ready" }
}

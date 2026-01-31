package com.quiverbot.application.controllers

import com.quiverbot.application.services.AlertingService
import com.quiverbot.application.services.ClassificationService
import com.quiverbot.application.services.IngestionService
import com.quiverbot.application.services.SummaryService
import com.quiverbot.domain.entities.EmailRecipient
import com.quiverbot.domain.entities.Signal
import com.quiverbot.domain.repositories.EmailRecipientRepository
import com.quiverbot.domain.repositories.SignalRepository
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

/**
 * Debug Controller
 *
 * Provides manual trigger endpoints for testing the application.
 * These endpoints allow you to trigger each job on-demand instead
 * of waiting for the cron schedule.
 *
 * Only available when DISABLE_CRON_JOBS=true (recommended for testing).
 * The endpoints will still work without it, but cron jobs will also run.
 *
 * Usage:
 *   1. Set DISABLE_CRON_JOBS=true in application.yml or environment
 *   2. Start the app
 *   3. Trigger jobs via POST requests (see README for curl examples)
 */
@RestController
@RequestMapping("/debug")
class DebugController(
    private val ingestionService: IngestionService,
    private val classificationService: ClassificationService,
    private val alertingService: AlertingService,
    private val summaryService: SummaryService,
    private val signalRepository: SignalRepository,
    private val emailRecipientRepository: EmailRecipientRepository,
    @Value("\${cron.disabled:false}") private val isDebugMode: Boolean
) {

    init {
        if (isDebugMode) {
            logger.info { "Debug endpoints enabled (cron.disabled=true)" }
        }
    }

    /**
     * Trigger tweet ingestion manually.
     *
     * POST /debug/trigger/ingestion
     *
     * This will fetch new tweets from Twitter and store them in the database.
     */
    @PostMapping("/trigger/ingestion")
    fun triggerIngestion(): TriggerResponse {
        logger.info { "Manual trigger: Ingestion" }

        return try {
            val count = ingestionService.ingestNewTweets()
            TriggerResponse(
                success = true,
                message = "Ingested $count new tweet(s)",
                count = count
            )
        } catch (e: Exception) {
            logger.error(e) { "Ingestion failed" }
            TriggerResponse(
                success = false,
                message = "Ingestion failed: ${e.message}",
                count = 0
            )
        }
    }

    /**
     * Trigger tweet classification manually.
     *
     * POST /debug/trigger/classification
     *
     * This will process unclassified tweets through the LLM.
     */
    @PostMapping("/trigger/classification")
    fun triggerClassification(): TriggerResponse {
        logger.info { "Manual trigger: Classification" }

        return try {
            val count = classificationService.processUnclassifiedTweets()
            TriggerResponse(
                success = true,
                message = "Created $count signal(s)",
                count = count
            )
        } catch (e: Exception) {
            logger.error(e) { "Classification failed" }
            TriggerResponse(
                success = false,
                message = "Classification failed: ${e.message}",
                count = 0
            )
        }
    }

    /**
     * Trigger alert sending manually.
     *
     * POST /debug/trigger/alerting
     *
     * This will send pending alerts for high-strength signals to all enabled channels.
     */
    @PostMapping("/trigger/alerting")
    fun triggerAlerting(): TriggerResponse {
        logger.info { "Manual trigger: Alerting" }

        return try {
            val count = alertingService.processPendingAlerts()
            val channels = alertingService.getEnabledChannels()
            TriggerResponse(
                success = true,
                message = "Sent alerts to $count channel(s): ${channels.joinToString()}",
                count = count
            )
        } catch (e: Exception) {
            logger.error(e) { "Alerting failed" }
            TriggerResponse(
                success = false,
                message = "Alerting failed: ${e.message}",
                count = 0
            )
        }
    }

    /**
     * Trigger daily summary manually.
     *
     * POST /debug/trigger/summary
     *
     * This will generate and send the daily summary email.
     */
    @PostMapping("/trigger/summary")
    fun triggerSummary(): TriggerResponse {
        logger.info { "Manual trigger: Daily Summary" }

        return try {
            val count = summaryService.generateAndSendSummary()
            val channels = summaryService.getEnabledChannels()
            TriggerResponse(
                success = true,
                message = "Sent summary to $count channel(s): ${channels.joinToString()}",
                count = count
            )
        } catch (e: Exception) {
            logger.error(e) { "Summary failed" }
            TriggerResponse(
                success = false,
                message = "Summary failed: ${e.message}",
                count = 0
            )
        }
    }

    /**
     * Get all signals from the database.
     *
     * GET /debug/signals
     * GET /debug/signals?isSignal=true  (only positive signals)
     * GET /debug/signals?isSignal=false (only negative signals)
     *
     * Returns classified signals, ordered by classification time (newest first).
     */
    @GetMapping("/signals")
    fun getAllSignals(
        @RequestParam(required = false) isSignal: Boolean?
    ): SignalsResponse {
        logger.info { "Fetching signals (filter: isSignal=$isSignal)" }

        val signals = signalRepository.findAll()
            .let { list ->
                when (isSignal) {
                    true -> list.filter { it.isSignal }
                    false -> list.filter { !it.isSignal }
                    null -> list
                }
            }

        return SignalsResponse(
            count = signals.size,
            signals = signals.map { it.toDto() }
        )
    }

    /**
     * Get all email recipients.
     *
     * GET /debug/recipients
     */
    @GetMapping("/recipients")
    fun getRecipients(): RecipientsResponse {
        logger.info { "Fetching email recipients" }
        val recipients = emailRecipientRepository.findAll()
        return RecipientsResponse(
            count = recipients.size,
            recipients = recipients.map { it.toDto() }
        )
    }

    /**
     * Add a new email recipient.
     *
     * POST /debug/recipients
     * Body: { "email": "user@example.com", "name": "User Name", "receivesAlerts": true, "receivesSummary": true }
     */
    @PostMapping("/recipients")
    fun addRecipient(@RequestBody request: AddRecipientRequest): RecipientResponse {
        logger.info { "Adding email recipient: ${request.email}" }

        return try {
            val recipient = emailRecipientRepository.save(
                email = request.email,
                name = request.name,
                receivesAlerts = request.receivesAlerts ?: true,
                receivesSummary = request.receivesSummary ?: true
            )
            RecipientResponse(
                success = true,
                message = "Recipient added: ${recipient.email}",
                recipient = recipient.toDto()
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to add recipient" }
            RecipientResponse(
                success = false,
                message = "Failed to add recipient: ${e.message}",
                recipient = null
            )
        }
    }

    /**
     * Delete an email recipient.
     *
     * DELETE /debug/recipients?email=user@example.com
     */
    @DeleteMapping("/recipients")
    fun deleteRecipient(@RequestParam email: String): TriggerResponse {
        logger.info { "Deleting email recipient: $email" }

        return try {
            val deleted = emailRecipientRepository.deleteByEmail(email)
            if (deleted) {
                TriggerResponse(
                    success = true,
                    message = "Recipient deleted: $email",
                    count = 1
                )
            } else {
                TriggerResponse(
                    success = false,
                    message = "Recipient not found: $email",
                    count = 0
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete recipient" }
            TriggerResponse(
                success = false,
                message = "Failed to delete recipient: ${e.message}",
                count = 0
            )
        }
    }

    /**
     * Run the full pipeline: ingestion → classification → alerting.
     *
     * POST /debug/trigger/full-pipeline
     *
     * This is a convenience endpoint that runs all three main jobs in sequence.
     */
    @PostMapping("/trigger/full-pipeline")
    fun triggerFullPipeline(): PipelineResponse {
        logger.info { "Manual trigger: Full Pipeline" }

        val results = PipelineResults(
            tweetsIngested = 0,
            signalsCreated = 0,
            channelsNotified = 0
        )

        return try {
            // Step 1: Ingest tweets
            logger.info { "Pipeline step 1/3: Ingestion" }
            results.tweetsIngested = ingestionService.ingestNewTweets()

            // Step 2: Classify tweets
            logger.info { "Pipeline step 2/3: Classification" }
            results.signalsCreated = classificationService.processUnclassifiedTweets()

            // Step 3: Send alerts
            logger.info { "Pipeline step 3/3: Alerting" }
            results.channelsNotified = alertingService.processPendingAlerts()

            PipelineResponse(
                success = true,
                message = "Pipeline complete: ${results.tweetsIngested} tweets → ${results.signalsCreated} signals → ${results.channelsNotified} channels notified",
                results = results
            )
        } catch (e: Exception) {
            logger.error(e) { "Pipeline failed" }
            PipelineResponse(
                success = false,
                message = "Pipeline failed: ${e.message}",
                results = results
            )
        }
    }
}

data class TriggerResponse(
    val success: Boolean,
    val message: String,
    val count: Int
)

data class PipelineResponse(
    val success: Boolean,
    val message: String,
    val results: PipelineResults
)

data class PipelineResults(
    var tweetsIngested: Int,
    var signalsCreated: Int,
    var channelsNotified: Int
)

data class SignalsResponse(
    val count: Int,
    val signals: List<SignalDto>
)

data class SignalDto(
    val id: String,
    val tweetId: String,
    val isSignal: Boolean,
    val signalStrength: Double,
    val category: String,
    val tickers: List<String>,
    val summary: String,
    val classifiedAt: String,
    val alertSent: Boolean,
    val includedInSummary: Boolean
)

private fun Signal.toDto() = SignalDto(
    id = id.toString(),
    tweetId = tweetId,
    isSignal = isSignal,
    signalStrength = signalStrength,
    category = category.name,
    tickers = tickers,
    summary = summary,
    classifiedAt = classifiedAt.toString(),
    alertSent = alertSent,
    includedInSummary = includedInSummary
)

data class AddRecipientRequest(
    val email: String,
    val name: String,
    val receivesAlerts: Boolean? = true,
    val receivesSummary: Boolean? = true
)

data class RecipientsResponse(
    val count: Int,
    val recipients: List<RecipientDto>
)

data class RecipientResponse(
    val success: Boolean,
    val message: String,
    val recipient: RecipientDto?
)

data class RecipientDto(
    val id: String,
    val email: String,
    val name: String,
    val receivesAlerts: Boolean,
    val receivesSummary: Boolean,
    val isActive: Boolean,
    val createdAt: String
)

private fun EmailRecipient.toDto() = RecipientDto(
    id = id.toString(),
    email = email,
    name = name,
    receivesAlerts = receivesAlerts,
    receivesSummary = receivesSummary,
    isActive = isActive,
    createdAt = createdAt.toString()
)

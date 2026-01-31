package com.quiverbot.infrastructure.email

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.quiverbot.domain.services.BatchSignalAlertEmailParams
import com.quiverbot.domain.services.DailySummaryEmailParams
import com.quiverbot.domain.services.EmailClient
import com.quiverbot.domain.services.SignalAlertEmailParams
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Sender.net email client implementation.
 *
 * Uses Sender API v2 direct send endpoint to send signal alerts and daily summaries.
 * Supports test mode for development (logs emails to console instead of sending).
 */
@Component
class SenderClient(
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    @Value("\${sender.api-key:}") private val apiKey: String,
    @Value("\${sender.from-email:}") private val fromEmail: String,
    @Value("\${sender.from-name:QuiverQuant Alerts}") private val fromName: String,
    @Value("\${email.test-mode:false}") private val testMode: Boolean
) : EmailClient {

    private val sendUrl = "https://api.sender.net/v2/message/send"

    init {
        if (testMode) {
            logger.info { "EMAIL_TEST_MODE enabled - emails will be logged to console instead of sent" }
        }

        if (!testMode && apiKey.isBlank()) {
            logger.warn { "SENDER_API_KEY not set - email sending will fail" }
        }

        if (!testMode && fromEmail.isBlank()) {
            logger.warn { "SENDER_FROM_EMAIL not set - email sending will fail" }
        }
    }

    override fun sendSignalAlert(params: SignalAlertEmailParams): String {
        val htmlBody = EmailTemplates.buildSignalAlertHtml(params)
        val textBody = EmailTemplates.buildSignalAlertText(params)

        return sendEmail(
            toEmail = params.to,
            toName = params.recipientName,
            subject = "QuiverQuant Signal Alert: ${params.category.replace("_", " ")}",
            html = htmlBody,
            text = textBody
        )
    }

    override fun sendBatchSignalAlert(params: BatchSignalAlertEmailParams): String {
        val htmlBody = EmailTemplates.buildBatchSignalAlertHtml(params)
        val textBody = EmailTemplates.buildBatchSignalAlertText(params)

        val signalCount = params.signals.size
        val subject = if (signalCount == 1) {
            "QuiverQuant Signal Alert: ${params.signals.first().category.replace("_", " ")}"
        } else {
            "QuiverQuant: $signalCount New Trading Signals"
        }

        return sendEmail(
            toEmail = params.to,
            toName = params.recipientName,
            subject = subject,
            html = htmlBody,
            text = textBody
        )
    }

    override fun sendDailySummary(params: DailySummaryEmailParams): String {
        val htmlBody = EmailTemplates.buildDailySummaryHtml(params)
        val textBody = EmailTemplates.buildDailySummaryText(params)

        return sendEmail(
            toEmail = params.to,
            toName = params.recipientName,
            subject = "QuiverQuant Daily Summary - ${params.summaryDate}",
            html = htmlBody,
            text = textBody
        )
    }

    override fun healthCheck(): Boolean {
        // Test mode always passes health check
        if (testMode) {
            logger.debug { "Health check passed (test mode)" }
            return true
        }

        if (apiKey.isBlank()) {
            logger.debug { "Health check failed: SENDER_API_KEY not set" }
            return false
        }

        if (fromEmail.isBlank()) {
            logger.debug { "Health check failed: SENDER_FROM_EMAIL not set" }
            return false
        }

        return true
    }

    private fun sendEmail(
        toEmail: String,
        toName: String,
        subject: String,
        html: String,
        text: String
    ): String {
        // Test mode: log email to console instead of sending
        if (testMode) {
            val messageId = "test_${System.currentTimeMillis()}"
            logger.info { "=".repeat(70) }
            logger.info { "[TEST MODE] Email would be sent:" }
            logger.info { "=".repeat(70) }
            logger.info { "From: $fromName <$fromEmail>" }
            logger.info { "To: $toName <$toEmail>" }
            logger.info { "Subject: $subject" }
            logger.info { "-".repeat(70) }
            logger.info { "Content (text):" }
            logger.info { text }
            logger.info { "-".repeat(70) }
            logger.info { "Message ID: $messageId" }
            logger.info { "=".repeat(70) }
            return messageId
        }

        // Production mode: actually send the email
        if (apiKey.isBlank()) {
            throw RuntimeException("SENDER_API_KEY not configured")
        }

        if (fromEmail.isBlank()) {
            throw RuntimeException("SENDER_FROM_EMAIL not configured")
        }

        val payload = mapOf(
            "from" to mapOf(
                "email" to fromEmail,
                "name" to fromName
            ),
            "to" to mapOf(
                "email" to toEmail,
                "name" to toName
            ),
            "subject" to subject,
            "html" to html,
            "text" to text
        )

        val jsonBody = objectMapper.writeValueAsString(payload)

        val request = Request.Builder()
            .url(sendUrl)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Accept", "application/json")
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"
                val data = objectMapper.readValue(body, SenderResponse::class.java)

                if (!response.isSuccessful) {
                    logger.error { "Sender API error: ${response.code} - $body" }
                    throw RuntimeException("Sender API error: ${response.code} - ${data.message ?: "Unknown error"}")
                }

                val messageId = data.id ?: "sender_${System.currentTimeMillis()}"
                logger.info { "Email sent via Sender: $messageId to $toEmail" }

                return messageId
            }
        } catch (e: Exception) {
            logger.error(e) { "Sender request failed" }
            throw e
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class SenderResponse(
    val id: String? = null,
    val message: String? = null,
    val success: Boolean? = null
)

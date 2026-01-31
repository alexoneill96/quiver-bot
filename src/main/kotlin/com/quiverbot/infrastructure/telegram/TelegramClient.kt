package com.quiverbot.infrastructure.telegram

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.quiverbot.domain.services.AlertNotificationData
import com.quiverbot.domain.services.NotificationChannel
import com.quiverbot.domain.services.SummaryNotificationData
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Telegram Bot API client for sending notifications.
 *
 * Implements the NotificationChannel interface to send alerts and summaries
 * via Telegram Bot API.
 */
@Component
class TelegramClient(
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    @Value("\${telegram.bot-token:}") private val botToken: String,
    @Value("\${telegram.chat-id:}") private val chatId: String,
    @Value("\${telegram.disable-preview:true}") private val disablePreview: Boolean,
    @Value("\${notify.telegram.enabled:false}") private val enabled: Boolean
) : NotificationChannel {

    override val channelName: String = "Telegram"

    private val baseUrl: String
        get() = "https://api.telegram.org/bot$botToken"

    init {
        if (enabled) {
            if (botToken.isBlank()) {
                logger.warn { "TELEGRAM_BOT_TOKEN not set - Telegram notifications will fail" }
            }
            if (chatId.isBlank()) {
                logger.warn { "TELEGRAM_CHAT_ID not set - Telegram notifications will fail" }
            }
            if (botToken.isNotBlank() && chatId.isNotBlank()) {
                logger.info { "Telegram notifications enabled for chat: $chatId" }
            }
        } else {
            logger.debug { "Telegram notifications disabled" }
        }
    }

    override fun sendAlert(data: AlertNotificationData): Boolean {
        if (!isConfigured()) {
            logger.warn { "Telegram not configured, skipping alert" }
            return false
        }

        val messages = TelegramMessageFormatter.formatAlert(data)
        return sendMessages(messages)
    }

    override fun sendDailySummary(data: SummaryNotificationData): Boolean {
        if (!isConfigured()) {
            logger.warn { "Telegram not configured, skipping summary" }
            return false
        }

        val messages = TelegramMessageFormatter.formatSummary(data)
        return sendMessages(messages)
    }

    override fun isConfigured(): Boolean {
        return enabled && botToken.isNotBlank() && chatId.isNotBlank()
    }

    /**
     * Send multiple messages (for long content that needs splitting).
     */
    private fun sendMessages(messages: List<String>): Boolean {
        var allSuccessful = true

        for (message in messages) {
            if (!sendMessage(message)) {
                allSuccessful = false
            }
        }

        return allSuccessful
    }

    /**
     * Send a single message via Telegram Bot API with Markdown formatting.
     */
    private fun sendMessage(text: String): Boolean {
        val payload = mapOf(
            "chat_id" to chatId,
            "text" to text,
            "parse_mode" to "Markdown",
            "disable_web_page_preview" to disablePreview
        )

        val jsonBody = objectMapper.writeValueAsString(payload)
        val url = "$baseUrl/sendMessage"

        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: "{}"

                if (!response.isSuccessful) {
                    val errorResponse = try {
                        objectMapper.readValue(body, TelegramResponse::class.java)
                    } catch (e: Exception) {
                        null
                    }
                    logger.error { "Telegram API error: ${response.code} - ${errorResponse?.description ?: body}" }
                    return false
                }

                val data = objectMapper.readValue(body, TelegramResponse::class.java)
                if (data.ok == true) {
                    logger.info { "Telegram message sent successfully to chat $chatId" }
                    true
                } else {
                    logger.error { "Telegram API returned ok=false: ${data.description}" }
                    false
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send Telegram message" }
            false
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TelegramResponse(
    val ok: Boolean? = null,
    val description: String? = null,
    val result: Any? = null
)

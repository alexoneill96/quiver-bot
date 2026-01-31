package com.quiverbot.infrastructure.llm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.quiverbot.domain.enums.SignalCategory
import com.quiverbot.domain.services.ClassificationResult
import com.quiverbot.domain.services.LLMClient
import com.quiverbot.domain.services.SignalSummaryData
import mu.KotlinLogging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * OpenAI API client implementation for LLM operations.
 *
 * Uses the Chat Completions API with GPT-4 (configurable).
 * Handles JSON parsing and validation of classification results.
 */
@Component
class OpenAIClient(
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    @Value("\${openai.api-key:}") private val apiKey: String,
    @Value("\${openai.model:gpt-4-turbo-preview}") private val model: String,
    @Value("\${openai.prompt-version:V2}") private val promptVersionStr: String
) : LLMClient {

    private val baseUrl = "https://api.openai.com/v1"
    private val promptVersion: LLMPrompts.ClassificationPromptVersion

    init {
        if (apiKey.isBlank()) {
            logger.warn { "OPENAI_API_KEY not set - OpenAI calls will fail" }
        }

        promptVersion = try {
            LLMPrompts.ClassificationPromptVersion.valueOf(promptVersionStr.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.warn { "Invalid prompt version '$promptVersionStr', defaulting to V2" }
            LLMPrompts.ClassificationPromptVersion.V2
        }

        logger.info { "Using classification prompt version: $promptVersion" }
    }

    override fun classifyTweet(tweetText: String, tweetUrl: String): ClassificationResult {
        val response = chatCompletion(
            LLMPrompts.getClassificationSystemPrompt(promptVersion),
            LLMPrompts.classificationUserPrompt(tweetText, tweetUrl)
        )

        return parseClassificationResult(response)
    }

    override fun generateDailySummary(signals: List<SignalSummaryData>): String {
        if (signals.isEmpty()) {
            return "No significant trading signals were detected today."
        }

        val signalData = signals.map { s ->
            LLMPrompts.SignalData(
                summary = s.summary,
                tickers = s.tickers,
                category = s.category.name,
                signalStrength = s.signalStrength
            )
        }

        return chatCompletion(
            LLMPrompts.DAILY_SUMMARY_SYSTEM_PROMPT,
            LLMPrompts.dailySummaryUserPrompt(signalData)
        )
    }

    override fun healthCheck(): Boolean {
        if (apiKey.isBlank()) return false

        return try {
            val request = Request.Builder()
                .url("$baseUrl/models")
                .get()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun chatCompletion(systemPrompt: String, userPrompt: String): String {
        val requestBody = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt)
            ),
            "temperature" to 0.3,
            "max_tokens" to 500
        )

        val jsonBody = objectMapper.writeValueAsString(requestBody)

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = response.body?.string() ?: "Unknown error"
                throw RuntimeException("OpenAI API error: ${response.code} - $error")
            }

            val body = response.body?.string() ?: throw RuntimeException("No response body from OpenAI")
            val data = objectMapper.readValue(body, OpenAIResponse::class.java)
            val content = data.choices.firstOrNull()?.message?.content
                ?: throw RuntimeException("No content in OpenAI response")

            return content.trim()
        }
    }

    private fun parseClassificationResult(response: String): ClassificationResult {
        return try {
            // Try to extract JSON from the response (in case of markdown wrapping)
            val jsonStr = Regex("\\{[\\s\\S]*\\}").find(response)?.value ?: response

            val parsed = objectMapper.readValue(jsonStr, OpenAIClassificationResponse::class.java)

            ClassificationResult(
                isSignal = parsed.is_signal ?: false,
                signalStrength = (parsed.signal_strength ?: 0.0).coerceIn(0.0, 1.0),
                category = normalizeCategory(parsed.category),
                tickers = parsed.tickers?.map { it.uppercase().removePrefix("$") } ?: emptyList(),
                summary = parsed.summary ?: "No summary available"
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse LLM response: $response" }
            // Return a safe default for unparseable responses
            ClassificationResult(
                isSignal = false,
                signalStrength = 0.0,
                category = SignalCategory.LOW_SIGNAL,
                tickers = emptyList(),
                summary = "Classification failed - treating as low signal"
            )
        }
    }

    private fun normalizeCategory(category: String?): SignalCategory {
        val upper = category?.uppercase() ?: return SignalCategory.LOW_SIGNAL
        return try {
            SignalCategory.valueOf(upper)
        } catch (e: IllegalArgumentException) {
            SignalCategory.LOW_SIGNAL
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIResponse(
    val choices: List<OpenAIChoice> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIChoice(
    val message: OpenAIMessage? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIMessage(
    val content: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIClassificationResponse(
    val is_signal: Boolean? = null,
    val signal_strength: Double? = null,
    val category: String? = null,
    val tickers: List<String>? = null,
    val summary: String? = null
)

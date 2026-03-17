package com.splitease.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.splitease.model.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class GeminiService(
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${gemini.api.model}") private val model: String
) {
    private val logger = LoggerFactory.getLogger(GeminiService::class.java)
    private val restTemplate = RestTemplate(SimpleClientHttpRequestFactory().apply {
        setConnectTimeout(60000)
        setReadTimeout(60000)
    })
    private val mapper = jacksonObjectMapper()

    fun analyseBillText(rawText: String): BillAnalysisResponse {
        val prompt = """
            Analyze the following text from a restaurant bill and return a structured JSON response.
            The JSON should have the following format:
            {
              "items": [
                { "name": "Item name", "price": total_price_for_this_item, "qty": quantity, "unitPrice": unit_price }
              ],
              "taxes": [
                { "name": "Tax name (e.g. SGST 2.5%)", "amount": tax_amount }
              ],
              "subtotal": subtotal_amount,
              "totalAmount": grand_total_amount
            }
            
            Ensure prices are numbers. If quantity is not specified, assume 1.
            Only return the JSON, no other text.
            
            BILL TEXT:
            $rawText
        """.trimIndent()

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val requestBody = mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val entity = HttpEntity(requestBody, headers)
        val response = try {
            restTemplate.postForEntity(url, entity, Map::class.java)
        } catch (e: org.springframework.web.client.HttpClientErrorException) {
            println("=== GEMINI API ERROR ===")
            println("Status: ${e.statusCode}")
            println("Response: ${e.responseBodyAsString}")
            println("========================")
            logger.error("Gemini API error: {} - {}", e.statusCode, e.responseBodyAsString)
            throw e
        } catch (e: Exception) {
            println("=== GEMINI API CALL FAILED ===")
            println(e.message)
            logger.error("Gemini API call failed", e)
            throw e
        }

        val responseBody = response.body as Map<*, *>
        val candidates = responseBody["candidates"] as List<*>
        val firstCandidate = candidates[0] as Map<*, *>
        val content = firstCandidate["content"] as Map<*, *>
        val parts = content["parts"] as List<*>
        val firstPart = parts[0] as Map<*, *>
        val text = firstPart["text"] as String
        println("=== RAW GEMINI RESPONSE ===")
        println(text)
        println("===========================")
        logger.debug("Raw Gemini response: {}", text)

        // Clean up the text in case Gemini wraps it in markdown code blocks
        val cleanJson = text.replace("```json", "").replace("```", "").trim()
        println("=== CLEANED JSON ===")
        println(cleanJson)
        println("====================")
        logger.debug("Cleaned JSON: {}", cleanJson)

        return try {
            val result = mapper.readValue<BillAnalysisResponse>(cleanJson)
            if (result.items.isEmpty() && result.totalAmount == 0.0) {
                logger.error("Gemini returned empty result. Raw JSON: {}", cleanJson)
                throw Exception("Gemini returned no items. Raw response: $cleanJson")
            }
            result
        } catch (e: Exception) {
            logger.error("Failed to parse JSON: {}. Error: {}", cleanJson, e.message)
            throw e
        }
    }
}

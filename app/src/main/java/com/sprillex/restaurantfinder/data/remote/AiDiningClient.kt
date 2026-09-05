package com.sprillex.restaurantfinder.data.remote

import android.util.Log
import com.sprillex.restaurantfinder.BuildConfig
import com.sprillex.restaurantfinder.data.Restaurant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AiDiningClient(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
) {
    companion object {
        private const val TAG = "AiDiningClient"
        private val CANDIDATE_ENDPOINTS = listOf(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
        )

        const val SYSTEM_INSTRUCTION =
            "You are a precise local dining assistant. Your task is to provide accurate, verified profile details for an independent restaurant based on real-world web grounding and local knowledge.\n\n" +
            "CRITICAL CONSTRAINTS:\n" +
            "1. ANCHORING: Ensure all information strictly matches the specific business located in or near the specified city/coordinates in Michigan or Ohio. Do not confuse this venue with same-named businesses in other regions.\n" +
            "2. HONESTY OVER COMPLETION: Independent and rural venues often have limited web footprints. If a specific field (such as signature items, parking, or patio) cannot be verified with confidence, return null or \"unknown\". NEVER invent or extrapolate menu items, policies, or hours.\n" +
            "3. OUTPUT FORMAT: Output ONLY valid, raw JSON adhering strictly to the requested schema. Do not include markdown code fences (```json), commentary, or preambles."
    }

    private fun logW(msg: String) {
        try { Log.w(TAG, msg) } catch (_: Throwable) { println("W/$TAG: $msg") }
    }

    private fun logE(msg: String, tr: Throwable? = null) {
        try { Log.e(TAG, msg, tr) } catch (_: Throwable) { println("E/$TAG: $msg ${tr?.message}") }
    }

    suspend fun validateApiKey(keyToTest: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val trimmedKey = keyToTest.trim()
        if (trimmedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be blank."))
        }

        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$trimmedKey")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                Result.success(true)
            } else {
                val errText = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                logE("Gemini API Key validation failed with code $responseCode: $errText")
                Result.failure(Exception("Validation failed (HTTP $responseCode)"))
            }
        } catch (e: Exception) {
            logE("Failed to validate Gemini API Key", e)
            Result.failure(e)
        }
    }

    suspend fun queryDiningProfile(
        restaurant: Restaurant,
        overrideApiKey: String? = null
    ): AiDiningResponse? = withContext(Dispatchers.IO) {
        val effectiveApiKey = overrideApiKey?.trim()?.ifBlank { null }
            ?: apiKey.trim().ifBlank { null }
            ?: ""

        if (effectiveApiKey.isBlank()) {
            logW("GEMINI_API_KEY is missing or blank. Skipping AI query.")
            return@withContext null
        }

        val userPrompt = buildUserPrompt(restaurant)
        val jsonRequestBody = buildRequestBody(SYSTEM_INSTRUCTION, userPrompt)

        for (endpoint in CANDIDATE_ENDPOINTS) {
            try {
                val url = URL("$endpoint?key=$effectiveApiKey")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                OutputStreamWriter(connection.outputStream, "UTF-8").use { os ->
                    os.write(jsonRequestBody)
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val extractedContent = parseGeminiResponseText(responseText) ?: continue
                    val cleanJson = sanitizeJsonString(extractedContent)
                    return@withContext json.decodeFromString<AiDiningResponse>(cleanJson)
                } else {
                    logW("Gemini API endpoint $endpoint returned HTTP $responseCode. Trying fallback...")
                }
            } catch (e: Exception) {
                logE("Failed request to Gemini endpoint: $endpoint", e)
            }
        }
        return@withContext null
    }

    fun buildUserPrompt(restaurant: Restaurant): String {
        val name = restaurant.name
        val amenity = restaurant.amenity
        val cuisine = restaurant.cuisine ?: "unknown"
        val street = listOfNotNull(restaurant.housenumber, restaurant.street)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .ifBlank { "unknown" }
        val city = restaurant.city ?: "unknown"
        val lat = restaurant.latitude
        val lng = restaurant.longitude

        return """
            Retrieve factual local profile details for the following venue:

            Business Name: $name
            Category: $amenity
            Known Cuisine: $cuisine
            Location: $street, $city, Region: SE Michigan / NW Ohio (Bounding Box ~41.25, -84.45 to 42.10, -83.25)
            Coordinates: $lat, $lng

            Respond using this exact JSON structure:
            {
              "found": true,
              "editorial_summary": "1-2 sentence overview of the establishment, cuisine focus, and general appeal.",
              "signature_items": ["Up to 3 specific standout dishes or drinks frequently mentioned by patrons"],
              "price_tier": "$ (Under $15/person) | $$ ($15-$30) | $$$ ($30-$60) | $$$$ ($60+) | unknown",
              "vibe_tags": ["2 to 4 lowercase tags describing atmosphere, e.g., dive_bar, rustic, cozy, sports_hub"],
              "service_model": "counter_service | full_table_service | bar_only | food_truck | unknown",
              "parking": "dedicated_lot | street_only | none | unknown",
              "outdoor_patio": "yes | no | unknown",
              "good_to_know": "One brief operational tip for visitors (e.g. 'Cash only', 'Peak waits during weekend breakfast', or null if none)"
            }

            If you cannot locate this specific business in this geographic area with reasonable certainty, return:
            {"found": false}
        """.trimIndent()
    }

    fun sanitizeJsonString(rawText: String): String {
        val firstBrace = rawText.indexOf('{')
        val lastBrace = rawText.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return rawText.substring(firstBrace, lastBrace + 1).trim()
        }
        var text = rawText.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json")
                .removePrefix("```JSON")
                .removePrefix("```")
                .trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }
        return text
    }

    private fun buildRequestBody(systemInstruction: String, userPrompt: String): String {
        return buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", systemInstruction) })
                }
            }
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject { put("text", userPrompt) })
                    }
                })
            }
            putJsonArray("tools") {
                add(buildJsonObject {
                    putJsonObject("googleSearch") {}
                })
            }
        }.toString()
    }

    private fun parseGeminiResponseText(responseText: String): String? {
        return try {
            val root = Json.parseToJsonElement(responseText)
            val candidates = root.jsonObject["candidates"]?.jsonArray
            val firstCandidate = candidates?.getOrNull(0)?.jsonObject
            val content = firstCandidate?.get("content")?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val firstPart = parts?.getOrNull(0)?.jsonObject
            firstPart?.get("text")?.jsonPrimitive?.content
        } catch (e: Exception) {
            logE("Error extracting text from Gemini response payload", e)
            null
        }
    }
}

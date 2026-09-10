package com.sprillex.restaurantfinder.data.remote

import com.sprillex.restaurantfinder.data.Restaurant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AiDiningClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testSanitizeJsonString() {
        val client = AiDiningClient(apiKey = "test_key")

        val rawWithMarkdown = """
            ```json
            {
              "found": true,
              "editorial_summary": "Test"
            }
            ```
        """.trimIndent()

        val clean = client.sanitizeJsonString(rawWithMarkdown)
        assertTrue(clean.startsWith("{"))
        assertTrue(clean.endsWith("}"))
        val response = json.decodeFromString<AiDiningResponse>(clean)
        assertTrue(response.found)
        assertEquals("Test", response.editorialSummary)
    }

    @Test
    fun testSanitizeJsonStringWithLeadingTextAndMarkdown() {
        val client = AiDiningClient(apiKey = "test_key")

        val rawWithProse = """
            Here is the requested information:
            ```json
            {
              "found": true,
              "editorial_summary": "Test"
            }
            ```
            Hope this helps!
        """.trimIndent()

        val clean = client.sanitizeJsonString(rawWithProse)
        assertTrue(clean.startsWith("{"))
        assertTrue(clean.endsWith("}"))
        val response = json.decodeFromString<AiDiningResponse>(clean)
        assertTrue(response.found)
        assertEquals("Test", response.editorialSummary)
    }

    @Test
    fun testBuildUserPromptContainsRestaurantData() {
        val client = AiDiningClient(apiKey = "test_key")
        val restaurant = Restaurant(
            id = "node/12345",
            osm_type = "node",
            osm_id = 12345,
            name = "Main Street Bistro",
            amenity = "restaurant",
            cuisine = "american",
            street = "Main St",
            housenumber = "100",
            postcode = "48104",
            city = "Ann Arbor",
            latitude = 42.2808,
            longitude = -83.7430
        )

        val prompt = client.buildUserPrompt(restaurant)
        assertTrue(prompt.contains("Main Street Bistro"))
        assertTrue(prompt.contains("american"))
        assertTrue(prompt.contains("100 Main St, Ann Arbor"))
        assertTrue(prompt.contains("42.2808, -83.743"))
    }

    @Test
    fun testBlankApiKeyReturnsNull() = runBlocking {
        val client = AiDiningClient(apiKey = "")
        val restaurant = Restaurant(
            id = "node/12345",
            name = "Test Restaurant",
            amenity = "restaurant",
            latitude = 42.0,
            longitude = -83.0
        )

        val result = client.queryDiningProfile(restaurant)
        assertNull(result)
    }

    @Test
    fun testValidateBlankApiKeyFails() = runBlocking {
        val client = AiDiningClient(apiKey = "")
        val result = client.validateApiKey("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun testParseAllTestResultsFixtures() {
        val client = AiDiningClient(apiKey = "test_key")
        val testResultsDir = File("test_results")
        if (!testResultsDir.exists() || !testResultsDir.isDirectory) return

        val files = testResultsDir.listFiles { _, name -> name.endsWith(".json") } ?: return

        for (file in files) {
            val fileContent = file.readText()
            val text = client.parseGeminiResponseText(fileContent)

            if (text != null) {
                val cleanJson = client.sanitizeJsonString(text)
                val parsedResponse = try {
                    json.decodeFromString<AiDiningResponse>(cleanJson)
                } catch (e: Exception) {
                    null
                }
                assertNotNull("File ${file.name} extracted text but failed to deserialize AiDiningResponse", parsedResponse)
                if (file.name.contains("632337651")) {
                    assertFalse("Expected found=false for ${file.name}", parsedResponse!!.found)
                } else {
                    assertTrue("Expected found=true for ${file.name}", parsedResponse!!.found)
                }
            } else {
                // If text is null, verify this file is an error payload or candidate without text parts
                assertTrue(
                    "File ${file.name} returned null text, but was expected to be an error payload or empty response",
                    fileContent.contains("\"error\"") || fileContent.contains("\"finishReason\"") || fileContent.contains("429")
                )
            }
        }
    }
}

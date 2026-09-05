package com.sprillex.restaurantfinder.data.remote

import com.sprillex.restaurantfinder.data.Restaurant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDiningClientTest {

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
}

package com.sprillex.restaurantfinder.data.remote

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDiningResponseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun testParseFullResponseJson() {
        val jsonPayload = """
            {
              "found": true,
              "editorial_summary": "Great local pizza place.",
              "signature_items": ["Pepperoni Pizza", "Garlic Knots"],
              "price_tier": "$$",
              "vibe_tags": ["casual", "family_friendly"],
              "service_model": "full_table_service",
              "parking": "dedicated_lot",
              "outdoor_patio": "yes",
              "good_to_know": "Gets busy on Friday nights"
            }
        """.trimIndent()

        val response = json.decodeFromString<AiDiningResponse>(jsonPayload)
        assertTrue(response.found)
        assertEquals("Great local pizza place.", response.editorialSummary)
        assertEquals(listOf("Pepperoni Pizza", "Garlic Knots"), response.signatureItems)
        assertEquals("$$", response.priceTier)
        assertEquals(listOf("casual", "family_friendly"), response.vibeTags)
        assertEquals("full_table_service", response.serviceModel)
        assertEquals("dedicated_lot", response.parking)
        assertEquals("yes", response.outdoorPatio)
        assertEquals("Gets busy on Friday nights", response.goodToKnow)
    }

    @Test
    fun testParseNotFoundResponseJson() {
        val jsonPayload = """{"found": false}"""
        val response = json.decodeFromString<AiDiningResponse>(jsonPayload)
        assertFalse(response.found)
        assertNull(response.editorialSummary)
        assertNull(response.signatureItems)
    }

    @Test
    fun testParseWithUnknownFields() {
        val jsonPayload = """
            {
              "found": true,
              "editorial_summary": "Charming cafe",
              "extra_unknown_field": "some_value"
            }
        """.trimIndent()

        val response = json.decodeFromString<AiDiningResponse>(jsonPayload)
        assertTrue(response.found)
        assertEquals("Charming cafe", response.editorialSummary)
    }
}

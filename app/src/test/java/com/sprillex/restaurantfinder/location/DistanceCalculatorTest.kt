package com.sprillex.restaurantfinder.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun testHaversineDistance_KnownCoordinates() {
        // Toledo, OH (41.6528, -83.5379) to Perrysburg, OH (41.5570, -83.6272)
        // Expected distance ~8-10 miles
        val distance = DistanceCalculator.calculateDistanceMiles(
            41.6528, -83.5379,
            41.5570, -83.6272
        )

        assertTrue(distance > 7.0 && distance < 10.0)
    }

    @Test
    fun testHaversineDistance_SameLocation() {
        val distance = DistanceCalculator.calculateDistanceMiles(
            41.6528, -83.5379,
            41.6528, -83.5379
        )

        assertEquals(0.0, distance, 0.0001)
    }

    @Test
    fun testFormatDistance_ShortDistanceFeet() {
        val formatted = DistanceCalculator.formatDistance(0.1) // 528 feet
        assertEquals("528 ft", formatted)
    }

    @Test
    fun testFormatDistance_MediumDistanceMiles() {
        val formatted = DistanceCalculator.formatDistance(2.34)
        assertEquals("2.3 mi", formatted)
    }

    @Test
    fun testFormatDistance_LongDistanceMiles() {
        val formatted = DistanceCalculator.formatDistance(15.7)
        assertEquals("16 mi", formatted)
    }
}

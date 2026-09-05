package com.sprillex.restaurantfinder.utils

import com.sprillex.restaurantfinder.data.Restaurant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentHelperTest {

    @Test
    fun formatShareText_formatsAllAvailableFields() {
        val restaurant = Restaurant(
            id = 12345L,
            name = "Test Bistro",
            amenity = "restaurant",
            cuisine = "Italian",
            street = "Main Street",
            housenumber = "100",
            postcode = "43604",
            city = "Toledo",
            phone = "+1-419-555-0100",
            website = "https://testbistro.com",
            latitude = 41.6528,
            longitude = -83.5379,
            last_updated = System.currentTimeMillis()
        )

        val shareText = IntentHelper.formatShareText(restaurant)

        assertTrue(shareText.contains("Check out Test Bistro!"))
        assertTrue(shareText.contains("Category: Italian"))
        assertTrue(shareText.contains("Address: 100 Main Street, Toledo, 43604"))
        assertTrue(shareText.contains("Phone: +1-419-555-0100"))
        assertTrue(shareText.contains("Website: https://testbistro.com"))
        assertTrue(shareText.contains("Location: https://maps.google.com/?q=41.6528,-83.5379"))
    }

    @Test
    fun formatShareText_handlesMissingOptionalFields() {
        val restaurant = Restaurant(
            id = 67890L,
            name = "Minimal Cafe",
            amenity = "cafe",
            cuisine = null,
            street = null,
            housenumber = null,
            postcode = null,
            city = null,
            phone = null,
            website = null,
            latitude = 41.5000,
            longitude = -83.6000,
            last_updated = System.currentTimeMillis()
        )

        val shareText = IntentHelper.formatShareText(restaurant)

        assertTrue(shareText.contains("Check out Minimal Cafe!"))
        assertTrue(shareText.contains("Category: Cafe"))
        assertTrue(!shareText.contains("Address:"))
        assertTrue(!shareText.contains("Phone:"))
        assertTrue(!shareText.contains("Website:"))
        assertTrue(shareText.contains("Location: https://maps.google.com/?q=41.5,-83.6"))
    }
}

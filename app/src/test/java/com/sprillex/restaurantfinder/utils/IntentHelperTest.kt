package com.sprillex.restaurantfinder.utils

import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.VisitLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentHelperTest {

    @Test
    fun formatShareText_formatsAllAvailableFields() {
        val restaurant = Restaurant(
            id = "node/12345",
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
            id = "way/67890",
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

    @Test
    fun formatVisitLogShareText_includesRatingDishesAndNotes() {
        val restaurant = Restaurant(
            id = "node/12345",
            name = "Test Bistro",
            amenity = "restaurant",
            latitude = 41.6528,
            longitude = -83.5379
        )
        val visit = VisitLog(
            id = 1L,
            restaurantId = restaurant.id,
            visitedAt = 1700000000000L,
            rating = 4,
            notes = "Quick lunch on patio",
            orderedDishes = "Reuben, Iced Tea"
        )

        val text = IntentHelper.formatVisitLogShareText(restaurant, visit, dateLabel = "Nov 14, 2023")

        assertTrue(text.contains("Visit to Test Bistro"))
        assertTrue(text.contains("Date: Nov 14, 2023"))
        assertTrue(text.contains("★★★★☆ (4/5)"))
        assertTrue(text.contains("Ordered: Reuben, Iced Tea"))
        assertTrue(text.contains("Notes: Quick lunch on patio"))
    }

    @Test
    fun formatDishShareText_handlesFavoriteAndWishlistVariants() {
        val restaurant = Restaurant(
            id = "way/99",
            name = "Noodle House",
            amenity = "restaurant",
            latitude = 41.65,
            longitude = -83.54
        )

        val favText = IntentHelper.formatDishShareText(
            restaurant = restaurant,
            dishName = "Tonkotsu Ramen",
            notes = "Rich broth",
            isFavorite = true
        )
        assertTrue(favText.contains("Favorite dish at Noodle House: Tonkotsu Ramen"))
        assertTrue(favText.contains("Notes: Rich broth"))

        val wishText = IntentHelper.formatDishShareText(
            restaurant = restaurant,
            dishName = "Karaage",
            notes = "",
            isFavorite = false
        )
        assertTrue(wishText.contains("Dish to try at Noodle House: Karaage"))
        assertFalse(wishText.contains("Notes:"))
    }
}

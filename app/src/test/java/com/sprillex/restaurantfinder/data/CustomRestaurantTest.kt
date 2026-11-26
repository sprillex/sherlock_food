package com.sprillex.restaurantfinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomRestaurantTest {

    @Test
    fun generatedIdUsesCustomPrefixAndConvertsToRestaurant() {
        val custom = CustomRestaurant(
            name = "Neighborhood Pho",
            amenity = "restaurant",
            cuisine = "vietnamese",
            street = "Monroe St",
            housenumber = "420",
            city = "Toledo",
            postcode = "43604",
            phone = "419-555-0199",
            website = "https://pho.example.com",
            openingHours = "Mo-Sa 11:00-21:00",
            takeaway = "yes",
            delivery = "no",
            outdoorSeating = "yes",
            driveThrough = "no",
            wheelchair = "yes",
            dietVegetarian = "yes",
            dietVegan = "yes",
            dietGlutenFree = "yes",
            latitude = 41.655,
            longitude = -83.541,
            createdAt = 123456L
        )

        assertTrue(CustomRestaurant.isCustomId(custom.id))
        assertFalse(CustomRestaurant.isCustomId("node/12345"))

        val mapped = custom.toRestaurant()
        assertEquals(custom.id, mapped.id)
        assertEquals("custom", mapped.osm_type)
        assertEquals(0L, mapped.osm_id)
        assertEquals("Neighborhood Pho", mapped.name)
        assertEquals("vietnamese", mapped.cuisine)
        assertEquals("yes", mapped.diet_vegan)
        assertEquals(41.655, mapped.latitude, 0.0)
        assertEquals(-83.541, mapped.longitude, 0.0)
        assertEquals(123456L, mapped.last_updated)
    }

    @Test
    fun mergeVenueListsIncludesCustomRestaurantsAndDeduplicatesById() {
        val osm = listOf(
            Restaurant(
                id = "node/1",
                name = "OSM Spot",
                amenity = "cafe",
                latitude = 41.65,
                longitude = -83.53
            )
        )
        val custom = listOf(
            CustomRestaurant(
                id = "custom/1",
                name = "Custom Bakery",
                amenity = "cafe",
                latitude = 41.66,
                longitude = -83.54
            )
        )

        val merged = mergeVenueLists(osm, custom)
        assertEquals(listOf("custom/1", "node/1"), merged.map { it.id })
    }
}

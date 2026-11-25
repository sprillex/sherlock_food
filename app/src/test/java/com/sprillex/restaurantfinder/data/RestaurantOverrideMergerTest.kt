package com.sprillex.restaurantfinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class RestaurantOverrideMergerTest {

    private fun baseRestaurant() = Restaurant(
        id = "node/12345",
        osm_type = "node",
        osm_id = 12345L,
        name = "Original Diner",
        amenity = "restaurant",
        cuisine = "american",
        street = "Main St",
        housenumber = "100",
        postcode = "43604",
        city = "Toledo",
        phone = "419-555-0100",
        website = "https://original.example.com",
        opening_hours = "Mo-Fr 08:00-20:00",
        takeaway = "yes",
        delivery = "no",
        outdoor_seating = "no",
        drive_through = "no",
        wheelchair = "limited",
        diet_vegetarian = "yes",
        diet_vegan = "no",
        diet_gluten_free = "no",
        latitude = 41.6528,
        longitude = -83.5379,
        last_updated = 1000L
    )

    @Test
    fun nullOverrideReturnsSameBaseInstance() {
        val base = baseRestaurant()
        val merged = mergeRestaurantWithOverride(base, null)
        assertSame(base, merged)
    }

    @Test
    fun overlaysNonNullFieldsAndPreservesImmutableIdentityAndCoordinates() {
        val base = baseRestaurant()
        val override = RestaurantOverride(
            restaurantId = base.id,
            name = "Updated Bistro",
            amenity = "cafe",
            cuisine = "french;bakery",
            street = "Summit St",
            housenumber = "202",
            postcode = "43605",
            city = "Perrysburg",
            phone = "419-555-9999",
            website = "https://updated.example.com",
            openingHours = "24/7",
            takeaway = "only",
            delivery = "yes",
            outdoorSeating = "yes",
            driveThrough = "yes",
            wheelchair = "yes",
            dietVegetarian = "only",
            dietVegan = "yes",
            dietGlutenFree = "yes",
            isClosed = false
        )

        val merged = base.applyOverride(override)

        // Immutable fields MUST remain identical
        assertEquals("node/12345", merged.id)
        assertEquals("node", merged.osm_type)
        assertEquals(12345L, merged.osm_id)
        assertEquals(41.6528, merged.latitude, 0.0)
        assertEquals(-83.5379, merged.longitude, 0.0)

        // Overridden fields
        assertEquals("Updated Bistro", merged.name)
        assertEquals("cafe", merged.amenity)
        assertEquals("french;bakery", merged.cuisine)
        assertEquals("Summit St", merged.street)
        assertEquals("202", merged.housenumber)
        assertEquals("43605", merged.postcode)
        assertEquals("Perrysburg", merged.city)
        assertEquals("419-555-9999", merged.phone)
        assertEquals("https://updated.example.com", merged.website)
        assertEquals("24/7", merged.opening_hours)
        assertEquals("only", merged.takeaway)
        assertEquals("yes", merged.delivery)
        assertEquals("yes", merged.outdoor_seating)
        assertEquals("yes", merged.drive_through)
        assertEquals("yes", merged.wheelchair)
        assertEquals("only", merged.diet_vegetarian)
        assertEquals("yes", merged.diet_vegan)
        assertEquals("yes", merged.diet_gluten_free)
    }

    @Test
    fun nullOverrideFieldsFallBackToBaseAndBlankNameIsIgnored() {
        val base = baseRestaurant()
        val override = RestaurantOverride(
            restaurantId = base.id,
            name = "   ",
            amenity = "",
            phone = "419-555-7777"
        )

        val merged = mergeRestaurantWithOverride(base, override)

        assertEquals("Original Diner", merged.name)
        assertEquals("restaurant", merged.amenity)
        assertEquals("419-555-7777", merged.phone)
        assertEquals("american", merged.cuisine)
        assertEquals("Mo-Fr 08:00-20:00", merged.opening_hours)
    }

    @Test
    fun clearingOptionalFieldWithEmptyStringOverridesBaseValue() {
        val base = baseRestaurant()
        val override = RestaurantOverride(
            restaurantId = base.id,
            phone = ""
        )

        val merged = mergeRestaurantWithOverride(base, override)
        assertEquals("", merged.phone)
        assertNull(override.website)
        assertEquals("https://original.example.com", merged.website)
    }
}

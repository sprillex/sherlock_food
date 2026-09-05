package com.sprillex.restaurantfinder.location

import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.ui.screens.sortAndFilterRestaurants
import org.junit.Assert.assertEquals
import org.junit.Test

class ProximitySortingTest {

    @Test
    fun proximitySorting_ordersRestaurantsAscendingByDistance() {
        val anchor = AnchorLocation("Toledo, OH", 41.6528, -83.5379)

        val closeRestaurant = Restaurant(
            id = 1, name = "Close Diner", amenity = "restaurant", cuisine = null,
            street = null, housenumber = null, postcode = null, city = "Toledo",
            phone = null, website = null, latitude = 41.6530, longitude = -83.5380, last_updated = 0
        )

        val farRestaurant = Restaurant(
            id = 2, name = "Far Bistro", amenity = "restaurant", cuisine = null,
            street = null, housenumber = null, postcode = null, city = "Adrian",
            phone = null, website = null, latitude = 41.8975, longitude = -84.0372, last_updated = 0
        )

        val restaurants = listOf(farRestaurant, closeRestaurant)

        val sorted = sortAndFilterRestaurants(
            restaurants = restaurants,
            favoriteIds = emptySet(),
            wishlistMap = emptyMap(),
            searchQuery = "",
            selectedAmenity = null,
            selectedAnchor = anchor
        )

        assertEquals("Close Diner", sorted[0].first.name)
        assertEquals("Far Bistro", sorted[1].first.name)
    }
}

package com.sprillex.restaurantfinder.ui.screens

import com.sprillex.restaurantfinder.data.CustomRestaurant
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantOverride
import com.sprillex.restaurantfinder.data.Wishlist
import com.sprillex.restaurantfinder.data.mergeVenueLists
import com.sprillex.restaurantfinder.location.AnchorLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenFilterTest {

    private val toledoAnchor = AnchorLocation("Toledo, OH", 41.6528, -83.5379)

    private fun sampleRestaurants() = listOf(
        Restaurant(
            id = "node/1",
            name = "Alpha Cafe",
            amenity = "cafe",
            cuisine = "coffee",
            opening_hours = "06:00-20:00",
            latitude = 41.6528,
            longitude = -83.5379
        ),
        Restaurant(
            id = "node/2",
            name = "Beta Tacos",
            amenity = "restaurant",
            cuisine = "mexican",
            opening_hours = "11:00-22:00",
            latitude = 41.6600,
            longitude = -83.5379
        )
    )

    @Test
    fun overrideValuesApplyBeforeSearchAndAmenityFilter() {
        val overrides = mapOf(
            "node/1" to RestaurantOverride(
                restaurantId = "node/1",
                name = "Alpha Bakery & Diner",
                amenity = "restaurant",
                cuisine = "bakery"
            )
        )

        val byName = sortAndFilterRestaurants(
            restaurants = sampleRestaurants(),
            favoriteIds = emptySet(),
            wishlistMap = emptyMap(),
            searchQuery = "Bakery",
            selectedAmenity = "restaurant",
            selectedAnchor = toledoAnchor,
            overridesMap = overrides
        )

        assertEquals(1, byName.size)
        assertEquals("Alpha Bakery & Diner", byName.first().first.name)
        assertEquals("restaurant", byName.first().first.amenity)
    }

    @Test
    fun openNowAndVisitedFiltersRespectOverridesAndClosedFlag() {
        val overrides = mapOf(
            "node/1" to RestaurantOverride(
                restaurantId = "node/1",
                isClosed = true
            )
        )

        val openResults = sortAndFilterRestaurants(
            restaurants = sampleRestaurants(),
            favoriteIds = emptySet(),
            wishlistMap = emptyMap(),
            searchQuery = "",
            selectedAmenity = "Open Now",
            selectedAnchor = toledoAnchor,
            overridesMap = overrides,
            openEvaluator = { _, ov ->
                if (ov?.isClosed == true) false else true
            }
        )
        assertEquals(listOf("node/2"), openResults.map { it.first.id })

        val visitedResults = sortAndFilterRestaurants(
            restaurants = sampleRestaurants(),
            favoriteIds = emptySet(),
            wishlistMap = emptyMap(),
            searchQuery = "",
            selectedAmenity = "Visited",
            selectedAnchor = toledoAnchor,
            visitedIds = setOf("node/1")
        )
        assertEquals(listOf("node/1"), visitedResults.map { it.first.id })
    }

    @Test
    fun wantToTryAndWishlistFiltersAndCustomRestaurantsSortCorrectly() {
        val customVenue = CustomRestaurant(
            id = "custom/pop-up-1",
            name = "Pop-Up Ramen",
            amenity = "restaurant",
            cuisine = "japanese",
            city = "Toledo",
            latitude = 41.6528, // exact anchor match -> 0.0 mi (first!)
            longitude = -83.5379
        )
        val merged = mergeVenueLists(sampleRestaurants(), listOf(customVenue))
        val wishlistMap = mapOf(
            "custom/pop-up-1" to Wishlist(
                restaurantId = "custom/pop-up-1",
                notes = "Spicy miso",
                priority = "High"
            ),
            "node/2" to Wishlist(
                restaurantId = "node/2",
                notes = "Birria tacos",
                priority = "Medium"
            )
        )

        val wantToTryResults = sortAndFilterRestaurants(
            restaurants = merged,
            favoriteIds = emptySet(),
            wishlistMap = wishlistMap,
            searchQuery = "",
            selectedAmenity = "Want to Try",
            selectedAnchor = toledoAnchor
        )
        assertEquals(listOf("custom/pop-up-1", "node/2"), wantToTryResults.map { it.first.id })

        val legacyWishlistAliasResults = sortAndFilterRestaurants(
            restaurants = merged,
            favoriteIds = emptySet(),
            wishlistMap = wishlistMap,
            searchQuery = "Ramen",
            selectedAmenity = "Wishlist",
            selectedAnchor = toledoAnchor
        )
        assertEquals(listOf("custom/pop-up-1"), legacyWishlistAliasResults.map { it.first.id })
    }
}

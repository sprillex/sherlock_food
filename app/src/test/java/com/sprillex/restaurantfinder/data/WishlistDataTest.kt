package com.sprillex.restaurantfinder.data

import com.sprillex.restaurantfinder.data.Wishlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WishlistDataTest {

    @Test
    fun wishlist_defaultValuesAndProperties() {
        val wishlist = Wishlist(
            restaurantId = "node/1001",
            notes = "Must try taco Tuesday",
            priority = "High"
        )

        assertEquals("node/1001", wishlist.restaurantId)
        assertEquals("Must try taco Tuesday", wishlist.notes)
        assertEquals("High", wishlist.priority)
        assertTrue(wishlist.addedAt > 0)
    }

    @Test
    fun wishlist_priorityDefaultsToMedium() {
        val wishlist = Wishlist(
            restaurantId = "node/2002"
        )

        assertEquals("Medium", wishlist.priority)
        assertEquals("", wishlist.notes)
    }
}

package com.sprillex.restaurantfinder.data

import com.sprillex.restaurantfinder.data.BackupManager
import com.sprillex.restaurantfinder.data.Wishlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WishlistDataTest {

    @Test
    fun wishlist_defaultValuesAndProperties() {
        val wishlist = Wishlist(
            restaurantId = 1001L,
            notes = "Must try taco Tuesday",
            priority = "High"
        )

        assertEquals(1001L, wishlist.restaurantId)
        assertEquals("Must try taco Tuesday", wishlist.notes)
        assertEquals("High", wishlist.priority)
        assertTrue(wishlist.addedAt > 0)
    }

    @Test
    fun wishlist_priorityDefaultsToMedium() {
        val wishlist = Wishlist(
            restaurantId = 2002L
        )

        assertEquals("Medium", wishlist.priority)
        assertEquals("", wishlist.notes)
    }
}

package com.sprillex.restaurantfinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DishWishlistTest {

    @Test
    fun dishWishlist_propertiesAndDefaults() {
        val dish = DishWishlist(
            restaurantId = "node/101",
            dishName = "Tonkotsu Ramen",
            notes = "Extra chashu pork"
        )

        assertEquals(0L, dish.id)
        assertEquals("node/101", dish.restaurantId)
        assertEquals("Tonkotsu Ramen", dish.dishName)
        assertEquals("Extra chashu pork", dish.notes)
        assertTrue(dish.addedAt > 0)
    }

    @Test
    fun dishWishlist_defaultNotesIsEmpty() {
        val dish = DishWishlist(
            restaurantId = "node/202",
            dishName = "Margherita Pizza"
        )

        assertEquals("", dish.notes)
    }
}

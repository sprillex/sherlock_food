package com.sprillex.restaurantfinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteDishTest {

    @Test
    fun favoriteDish_propertiesAndDefaults() {
        val favDish = FavoriteDish(
            restaurantId = 303L,
            dishName = "Smoked Beef Brisket",
            notes = "Extra barbecue sauce on side"
        )

        assertEquals(0L, favDish.id)
        assertEquals(303L, favDish.restaurantId)
        assertEquals("Smoked Beef Brisket", favDish.dishName)
        assertEquals("Extra barbecue sauce on side", favDish.notes)
        assertTrue(favDish.addedAt > 0)
    }

    @Test
    fun favoriteDish_defaultNotesIsEmpty() {
        val favDish = FavoriteDish(
            restaurantId = 404L,
            dishName = "Classic Cheeseburger"
        )

        assertEquals("", favDish.notes)
    }
}

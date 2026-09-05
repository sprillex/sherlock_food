package com.sprillex.restaurantfinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteDishTest {

    @Test
    fun favoriteDish_propertiesAndDefaults() {
        val favDish = FavoriteDish(
            restaurantId = "node/303",
            dishName = "Smoked Beef Brisket",
            notes = "Extra barbecue sauce on side"
        )

        assertEquals(0L, favDish.id)
        assertEquals("node/303", favDish.restaurantId)
        assertEquals("Smoked Beef Brisket", favDish.dishName)
        assertEquals("Extra barbecue sauce on side", favDish.notes)
        assertTrue(favDish.addedAt > 0)
    }

    @Test
    fun favoriteDish_defaultNotesIsEmpty() {
        val favDish = FavoriteDish(
            restaurantId = "node/404",
            dishName = "Classic Cheeseburger"
        )

        assertEquals("", favDish.notes)
    }
}

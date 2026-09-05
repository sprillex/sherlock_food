package com.sprillex.restaurantfinder.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StringListConvertersTest {

    private val converters = StringListConverters()

    @Test
    fun testFromStringList_nullAndEmpty() {
        assertNull(converters.fromStringList(null))
        assertEquals("[]", converters.fromStringList(emptyList()))
    }

    @Test
    fun testFromStringList_validList() {
        val list = listOf("casual", "cozy", "family_friendly")
        val json = converters.fromStringList(list)
        assertEquals("[\"casual\",\"cozy\",\"family_friendly\"]", json)
    }

    @Test
    fun testToStringList_nullAndBlank() {
        assertNull(converters.toStringList(null))
        assertNull(converters.toStringList(""))
        assertNull(converters.toStringList("   "))
    }

    @Test
    fun testToStringList_validJson() {
        val json = "[\"Item 1\",\"Item 2\"]"
        val list = converters.toStringList(json)
        assertEquals(listOf("Item 1", "Item 2"), list)
    }
}

package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurant_overrides")
data class RestaurantOverride(
    @PrimaryKey val restaurantId: String,
    val name: String? = null,
    val amenity: String? = null,
    val cuisine: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val openingHours: String? = null,
    val takeaway: String? = null,
    val delivery: String? = null,
    val outdoorSeating: String? = null,
    val driveThrough: String? = null,
    val wheelchair: String? = null,
    val dietVegetarian: String? = null,
    val dietVegan: String? = null,
    val dietGlutenFree: String? = null,
    val isClosed: Boolean? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

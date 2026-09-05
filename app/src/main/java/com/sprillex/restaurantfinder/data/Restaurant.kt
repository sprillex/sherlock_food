package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "restaurants")
data class Restaurant(
    @PrimaryKey val id: Long,
    val name: String,
    val amenity: String,
    val cuisine: String?,
    val street: String?,
    val housenumber: String?,
    val postcode: String?,
    val city: String?,
    val phone: String?,
    val website: String?,
    val latitude: Double,
    val longitude: Double,
    val last_updated: Long
)

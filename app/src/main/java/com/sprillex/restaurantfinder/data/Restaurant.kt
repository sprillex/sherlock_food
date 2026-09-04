package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurants",
    indices = [
        Index(value = ["latitude", "longitude"], name = "index_restaurants_latitude_longitude"),
        Index(value = ["amenity"], name = "index_restaurants_amenity"),
        Index(value = ["cuisine"], name = "index_restaurants_cuisine"),
        Index(value = ["city"], name = "index_restaurants_city")
    ]
)
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

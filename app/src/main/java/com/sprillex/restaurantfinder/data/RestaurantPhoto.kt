package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "restaurant_photos",
    indices = [Index(value = ["restaurantId"], name = "idx_restaurant_photos_restaurantId")]
)
data class RestaurantPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restaurantId: String,
    val relativePath: String,
    val category: String = "Food", // "Food", "Menu", "Exterior", or "Interior"
    val caption: String = "",
    val addedAt: Long = System.currentTimeMillis()
) {
    companion object {
        val CATEGORIES = listOf("Food", "Menu", "Exterior", "Interior")
    }
}

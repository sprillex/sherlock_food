package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_wishlist")
data class Wishlist(
    @PrimaryKey val restaurantId: String,
    val notes: String = "",
    val priority: String = "Medium", // Low, Medium, High
    val addedAt: Long = System.currentTimeMillis()
)

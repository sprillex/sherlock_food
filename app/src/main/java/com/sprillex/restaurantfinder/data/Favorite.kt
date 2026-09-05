package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_favorites")
data class Favorite(
    @PrimaryKey val restaurantId: String,
    val addedAt: Long = System.currentTimeMillis()
)

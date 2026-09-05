package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dish_wishlist")
data class DishWishlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restaurantId: String,
    val dishName: String,
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

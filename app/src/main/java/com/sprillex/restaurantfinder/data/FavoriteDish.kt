package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_dishes")
data class FavoriteDish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restaurantId: Long,
    val dishName: String,
    val notes: String = "",
    val addedAt: Long = System.currentTimeMillis()
)

package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manual_insights")
data class ManualInsight(
    @PrimaryKey val restaurantId: String,
    val summary: String = "",
    val recommendedDishes: String = "",
    val priceTier: String = "",
    val parkingNotes: String = "",
    val goodToKnow: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

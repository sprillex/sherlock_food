package com.sprillex.restaurantfinder.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "visit_logs",
    indices = [Index(value = ["restaurantId"], name = "idx_visit_logs_restaurantId")]
)
data class VisitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val restaurantId: String,
    val visitedAt: Long = System.currentTimeMillis(),
    val rating: Int = 5, // 1..5
    val notes: String = "",
    val orderedDishes: String = ""
)

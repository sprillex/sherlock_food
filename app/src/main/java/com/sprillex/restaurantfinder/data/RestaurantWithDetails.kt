package com.sprillex.restaurantfinder.data

import androidx.room.Embedded
import androidx.room.Relation

data class RestaurantWithDetails(
    @Embedded val restaurant: Restaurant,
    @Relation(
        parentColumn = "id",
        entityColumn = "restaurant_id"
    )
    val details: RestaurantDetailEntity?
)

package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDao {
    @Query("SELECT * FROM restaurants")
    fun getAllRestaurants(): Flow<List<Restaurant>>

    @Query("SELECT * FROM restaurants WHERE id = :id")
    suspend fun getRestaurantById(id: Long): Restaurant?

    @Query("""
        SELECT * FROM restaurants
        WHERE latitude BETWEEN :minLat AND :maxLat
          AND longitude BETWEEN :minLng AND :maxLng
    """)
    fun getRestaurantsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double
    ): Flow<List<Restaurant>>

    @Query("""
        SELECT * FROM restaurants
        WHERE (name LIKE '%' || :query || '%' OR cuisine LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%')
    """)
    fun searchRestaurants(query: String): Flow<List<Restaurant>>

    @Upsert
    suspend fun upsertRestaurants(restaurants: List<Restaurant>)
}

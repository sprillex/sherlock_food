package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantDetailDao {

    @Transaction
    @Query("SELECT * FROM restaurants WHERE id = :restaurantId")
    fun getRestaurantWithDetailsFlow(restaurantId: String): Flow<RestaurantWithDetails?>

    @Query("SELECT * FROM restaurant_details WHERE restaurant_id = :restaurantId LIMIT 1")
    suspend fun getCachedDetails(restaurantId: String): RestaurantDetailEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetails(details: RestaurantDetailEntity)

    @Query("DELETE FROM restaurant_details WHERE restaurant_id = :restaurantId")
    suspend fun deleteDetails(restaurantId: String)
}

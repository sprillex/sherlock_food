package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRestaurantDao {
    @Query("SELECT * FROM custom_restaurants ORDER BY createdAt DESC")
    fun getAllCustomRestaurants(): Flow<List<CustomRestaurant>>

    @Query("SELECT * FROM custom_restaurants WHERE id = :id")
    suspend fun getCustomRestaurantById(id: String): CustomRestaurant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomRestaurant(customRestaurant: CustomRestaurant)

    @Delete
    suspend fun removeCustomRestaurant(customRestaurant: CustomRestaurant)

    @Query("DELETE FROM custom_restaurants WHERE id = :id")
    suspend fun deleteCustomRestaurantById(id: String)
}

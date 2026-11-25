package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantOverrideDao {
    @Query("SELECT * FROM restaurant_overrides")
    fun getAllOverrides(): Flow<List<RestaurantOverride>>

    @Query("SELECT * FROM restaurant_overrides WHERE restaurantId = :restaurantId")
    fun getOverrideForRestaurant(restaurantId: String): Flow<RestaurantOverride?>

    @Query("SELECT * FROM restaurant_overrides WHERE restaurantId = :restaurantId")
    suspend fun getOverride(restaurantId: String): RestaurantOverride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: RestaurantOverride)

    @Query("DELETE FROM restaurant_overrides WHERE restaurantId = :restaurantId")
    suspend fun deleteOverride(restaurantId: String)

    @Delete
    suspend fun removeOverride(override: RestaurantOverride)
}

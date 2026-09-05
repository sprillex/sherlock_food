package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDishDao {
    @Query("SELECT * FROM favorite_dishes WHERE restaurantId = :restaurantId")
    fun getFavoriteDishesForRestaurant(restaurantId: String): Flow<List<FavoriteDish>>

    @Query("SELECT * FROM favorite_dishes")
    fun getAllFavoriteDishes(): Flow<List<FavoriteDish>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdateFavoriteDish(favoriteDish: FavoriteDish)

    @Delete
    suspend fun removeFavoriteDish(favoriteDish: FavoriteDish)

    @Query("DELETE FROM favorite_dishes WHERE id = :dishId")
    suspend fun removeFavoriteDishById(dishId: Long)
}

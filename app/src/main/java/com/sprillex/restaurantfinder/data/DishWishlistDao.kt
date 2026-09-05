package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DishWishlistDao {
    @Query("SELECT * FROM dish_wishlist WHERE restaurantId = :restaurantId")
    fun getDishesForRestaurant(restaurantId: String): Flow<List<DishWishlist>>

    @Query("SELECT * FROM dish_wishlist")
    fun getAllDishWishlistItems(): Flow<List<DishWishlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdateDish(dish: DishWishlist)

    @Delete
    suspend fun removeDish(dish: DishWishlist)

    @Query("DELETE FROM dish_wishlist WHERE id = :dishId")
    suspend fun removeDishById(dishId: Long)
}

package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query("SELECT * FROM user_wishlist")
    fun getAllWishlistItems(): Flow<List<Wishlist>>

    @Query("SELECT * FROM user_wishlist WHERE restaurantId = :restaurantId")
    suspend fun getWishlistItem(restaurantId: Long): Wishlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdateWishlist(wishlist: Wishlist)

    @Delete
    suspend fun removeWishlist(wishlist: Wishlist)

    @Query("DELETE FROM user_wishlist WHERE restaurantId = :restaurantId")
    suspend fun removeWishlistByRestaurantId(restaurantId: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM user_wishlist WHERE restaurantId = :restaurantId)")
    fun isInWishlist(restaurantId: Long): Flow<Boolean>
}

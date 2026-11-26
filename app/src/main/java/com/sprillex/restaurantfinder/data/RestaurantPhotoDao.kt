package com.sprillex.restaurantfinder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RestaurantPhotoDao {
    @Query("SELECT * FROM restaurant_photos ORDER BY addedAt DESC")
    fun getAllPhotos(): Flow<List<RestaurantPhoto>>

    @Query("SELECT * FROM restaurant_photos WHERE restaurantId = :restaurantId ORDER BY addedAt DESC")
    fun getPhotosForRestaurant(restaurantId: String): Flow<List<RestaurantPhoto>>

    @Query("SELECT * FROM restaurant_photos WHERE restaurantId = :restaurantId ORDER BY addedAt DESC")
    suspend fun getPhotosListForRestaurant(restaurantId: String): List<RestaurantPhoto>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: RestaurantPhoto): Long

    @Update
    suspend fun updatePhoto(photo: RestaurantPhoto)

    @Delete
    suspend fun deletePhoto(photo: RestaurantPhoto)

    @Query("DELETE FROM restaurant_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)
}

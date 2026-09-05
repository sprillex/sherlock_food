package com.sprillex.restaurantfinder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Favorite::class, Wishlist::class, DishWishlist::class, FavoriteDish::class], version = 5, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun dishWishlistDao(): DishWishlistDao
    abstract fun favoriteDishDao(): FavoriteDishDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_data.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

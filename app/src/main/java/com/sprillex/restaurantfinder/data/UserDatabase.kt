package com.sprillex.restaurantfinder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Favorite::class,
        Wishlist::class,
        DishWishlist::class,
        FavoriteDish::class,
        RestaurantOverride::class,
        ManualInsight::class,
        RestaurantPhoto::class,
        VisitLog::class
    ],
    version = 6,
    exportSchema = false
)
abstract class UserDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun dishWishlistDao(): DishWishlistDao
    abstract fun favoriteDishDao(): FavoriteDishDao
    abstract fun restaurantOverrideDao(): RestaurantOverrideDao
    abstract fun manualInsightDao(): ManualInsightDao
    abstract fun restaurantPhotoDao(): RestaurantPhotoDao
    abstract fun visitLogDao(): VisitLogDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_overrides` (
                        `restaurantId` TEXT NOT NULL,
                        `name` TEXT,
                        `amenity` TEXT,
                        `cuisine` TEXT,
                        `street` TEXT,
                        `housenumber` TEXT,
                        `postcode` TEXT,
                        `city` TEXT,
                        `phone` TEXT,
                        `website` TEXT,
                        `openingHours` TEXT,
                        `takeaway` TEXT,
                        `delivery` TEXT,
                        `outdoorSeating` TEXT,
                        `driveThrough` TEXT,
                        `wheelchair` TEXT,
                        `dietVegetarian` TEXT,
                        `dietVegan` TEXT,
                        `dietGlutenFree` TEXT,
                        `isClosed` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`restaurantId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `manual_insights` (
                        `restaurantId` TEXT NOT NULL,
                        `summary` TEXT NOT NULL,
                        `recommendedDishes` TEXT NOT NULL,
                        `priceTier` TEXT NOT NULL,
                        `parkingNotes` TEXT NOT NULL,
                        `goodToKnow` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`restaurantId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `restaurant_photos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `restaurantId` TEXT NOT NULL,
                        `relativePath` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `caption` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_restaurant_photos_restaurantId` ON `restaurant_photos` (`restaurantId`)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `visit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `restaurantId` TEXT NOT NULL,
                        `visitedAt` INTEGER NOT NULL,
                        `rating` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `orderedDishes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `idx_visit_logs_restaurantId` ON `visit_logs` (`restaurantId`)"
                )
            }
        }

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_data.db"
                )
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.sprillex.restaurantfinder.data

import android.content.ContextWrapper
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    private class InMemorySharedPreferences : SharedPreferences {
        private val map = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = (map[key] as? String) ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) map[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) map[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) map.remove(key)
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                map.clear()
                return this
            }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private class TestContext(private val prefs: SharedPreferences) : ContextWrapper(null) {
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private class FakeFavoriteDao : FavoriteDao {
        val items = MutableStateFlow<List<Favorite>>(emptyList())
        override fun getAllFavorites(): Flow<List<Favorite>> = items
        override suspend fun addFavorite(favorite: Favorite) {
            items.value = items.value.filterNot { it.restaurantId == favorite.restaurantId } + favorite
        }
        override suspend fun removeFavorite(favorite: Favorite) {
            items.value = items.value.filterNot { it.restaurantId == favorite.restaurantId }
        }
        override fun isFavorite(restaurantId: String): Flow<Boolean> =
            items.map { list -> list.any { it.restaurantId == restaurantId } }
    }

    private class FakeWishlistDao : WishlistDao {
        val items = MutableStateFlow<List<Wishlist>>(emptyList())
        override fun getAllWishlistItems(): Flow<List<Wishlist>> = items
        override suspend fun getWishlistItem(restaurantId: String): Wishlist? =
            items.value.firstOrNull { it.restaurantId == restaurantId }
        override suspend fun addOrUpdateWishlist(wishlist: Wishlist) {
            items.value = items.value.filterNot { it.restaurantId == wishlist.restaurantId } + wishlist
        }
        override suspend fun removeWishlist(wishlist: Wishlist) {
            items.value = items.value.filterNot { it.restaurantId == wishlist.restaurantId }
        }
        override suspend fun removeWishlistByRestaurantId(restaurantId: String) {
            items.value = items.value.filterNot { it.restaurantId == restaurantId }
        }
        override fun isInWishlist(restaurantId: String): Flow<Boolean> =
            items.map { list -> list.any { it.restaurantId == restaurantId } }
    }

    private class FakeDishWishlistDao : DishWishlistDao {
        val items = MutableStateFlow<List<DishWishlist>>(emptyList())
        private var nextId = 1L
        override fun getDishesForRestaurant(restaurantId: String): Flow<List<DishWishlist>> =
            items.map { list -> list.filter { it.restaurantId == restaurantId } }
        override fun getAllDishWishlistItems(): Flow<List<DishWishlist>> = items
        override suspend fun addOrUpdateDish(dish: DishWishlist) {
            val withId = if (dish.id == 0L) dish.copy(id = nextId++) else dish
            items.value = items.value.filterNot { it.id == withId.id } + withId
        }
        override suspend fun removeDish(dish: DishWishlist) {
            items.value = items.value.filterNot { it.id == dish.id }
        }
        override suspend fun removeDishById(dishId: Long) {
            items.value = items.value.filterNot { it.id == dishId }
        }
    }

    private class FakeFavoriteDishDao : FavoriteDishDao {
        val items = MutableStateFlow<List<FavoriteDish>>(emptyList())
        private var nextId = 1L
        override fun getFavoriteDishesForRestaurant(restaurantId: String): Flow<List<FavoriteDish>> =
            items.map { list -> list.filter { it.restaurantId == restaurantId } }
        override fun getAllFavoriteDishes(): Flow<List<FavoriteDish>> = items
        override suspend fun addOrUpdateFavoriteDish(favoriteDish: FavoriteDish) {
            val withId = if (favoriteDish.id == 0L) favoriteDish.copy(id = nextId++) else favoriteDish
            items.value = items.value.filterNot { it.id == withId.id } + withId
        }
        override suspend fun removeFavoriteDish(favoriteDish: FavoriteDish) {
            items.value = items.value.filterNot { it.id == favoriteDish.id }
        }
        override suspend fun removeFavoriteDishById(dishId: Long) {
            items.value = items.value.filterNot { it.id == dishId }
        }
    }

    private class FakeOverrideDao : RestaurantOverrideDao {
        val items = MutableStateFlow<List<RestaurantOverride>>(emptyList())
        override fun getAllOverrides(): Flow<List<RestaurantOverride>> = items
        override fun getOverrideForRestaurant(restaurantId: String): Flow<RestaurantOverride?> =
            items.map { list -> list.firstOrNull { it.restaurantId == restaurantId } }
        override suspend fun getOverride(restaurantId: String): RestaurantOverride? =
            items.value.firstOrNull { it.restaurantId == restaurantId }
        override suspend fun upsertOverride(override: RestaurantOverride) {
            items.value = items.value.filterNot { it.restaurantId == override.restaurantId } + override
        }
        override suspend fun deleteOverride(restaurantId: String) {
            items.value = items.value.filterNot { it.restaurantId == restaurantId }
        }
        override suspend fun removeOverride(override: RestaurantOverride) {
            deleteOverride(override.restaurantId)
        }
    }

    private class FakeManualInsightDao : ManualInsightDao {
        val items = MutableStateFlow<List<ManualInsight>>(emptyList())
        override fun getAllManualInsights(): Flow<List<ManualInsight>> = items
        override fun getManualInsightForRestaurant(restaurantId: String): Flow<ManualInsight?> =
            items.map { list -> list.firstOrNull { it.restaurantId == restaurantId } }
        override suspend fun getManualInsight(restaurantId: String): ManualInsight? =
            items.value.firstOrNull { it.restaurantId == restaurantId }
        override suspend fun upsertManualInsight(insight: ManualInsight) {
            items.value = items.value.filterNot { it.restaurantId == insight.restaurantId } + insight
        }
        override suspend fun deleteManualInsight(restaurantId: String) {
            items.value = items.value.filterNot { it.restaurantId == restaurantId }
        }
        override suspend fun removeManualInsight(insight: ManualInsight) {
            deleteManualInsight(insight.restaurantId)
        }
    }

    private class FakePhotoDao : RestaurantPhotoDao {
        val items = MutableStateFlow<List<RestaurantPhoto>>(emptyList())
        private var nextId = 1L
        override fun getAllPhotos(): Flow<List<RestaurantPhoto>> = items
        override fun getPhotosForRestaurant(restaurantId: String): Flow<List<RestaurantPhoto>> =
            items.map { list -> list.filter { it.restaurantId == restaurantId } }
        override suspend fun getPhotosListForRestaurant(restaurantId: String): List<RestaurantPhoto> =
            items.value.filter { it.restaurantId == restaurantId }
        override suspend fun insertPhoto(photo: RestaurantPhoto): Long {
            val id = if (photo.id == 0L) nextId++ else photo.id
            val withId = photo.copy(id = id)
            items.value = items.value.filterNot { it.id == id } + withId
            return id
        }
        override suspend fun deletePhoto(photo: RestaurantPhoto) {
            items.value = items.value.filterNot { it.id == photo.id }
        }
        override suspend fun deletePhotoById(photoId: Long) {
            items.value = items.value.filterNot { it.id == photoId }
        }
    }

    private class FakeVisitLogDao : VisitLogDao {
        val items = MutableStateFlow<List<VisitLog>>(emptyList())
        private var nextId = 1L
        override fun getAllVisitLogs(): Flow<List<VisitLog>> = items
        override fun getVisitLogsForRestaurant(restaurantId: String): Flow<List<VisitLog>> =
            items.map { list -> list.filter { it.restaurantId == restaurantId } }
        override suspend fun getVisitLogsListForRestaurant(restaurantId: String): List<VisitLog> =
            items.value.filter { it.restaurantId == restaurantId }
        override suspend fun upsertVisitLog(visitLog: VisitLog): Long {
            val id = if (visitLog.id == 0L) nextId++ else visitLog.id
            val withId = visitLog.copy(id = id)
            items.value = items.value.filterNot { it.id == id } + withId
            return id
        }
        override suspend fun deleteVisitLog(visitLog: VisitLog) {
            items.value = items.value.filterNot { it.id == visitLog.id }
        }
        override suspend fun deleteVisitLogById(visitId: Long) {
            items.value = items.value.filterNot { it.id == visitId }
        }
    }

    private fun createDaoBundle(): BackupDaos {
        return BackupDaos(
            favoriteDao = FakeFavoriteDao(),
            wishlistDao = FakeWishlistDao(),
            dishWishlistDao = FakeDishWishlistDao(),
            favoriteDishDao = FakeFavoriteDishDao(),
            restaurantOverrideDao = FakeOverrideDao(),
            manualInsightDao = FakeManualInsightDao(),
            restaurantPhotoDao = FakePhotoDao(),
            visitLogDao = FakeVisitLogDao()
        )
    }

    @Test
    fun v6BackupRoundTripPreservesAllTablesAndSpecialCharacters() = runBlocking {
        val prefs = InMemorySharedPreferences()
        val context = TestContext(prefs)
        val sourceDaos = createDaoBundle()

        PreferenceManager(context).setSelectedAnchorIndex(3)
        sourceDaos.favoriteDao.addFavorite(Favorite("node/1", addedAt = 100L))
        sourceDaos.wishlistDao.addOrUpdateWishlist(Wishlist("way/2", notes = "Try \"special\" menu", priority = "High", addedAt = 200L))
        sourceDaos.dishWishlistDao.addOrUpdateDish(DishWishlist(restaurantId = "way/2", dishName = "Birria", notes = "Extra consomme", addedAt = 300L))
        sourceDaos.favoriteDishDao.addOrUpdateFavoriteDish(FavoriteDish(restaurantId = "node/1", dishName = "Reuben", notes = "Crispy rye", addedAt = 400L))
        sourceDaos.restaurantOverrideDao.upsertOverride(
            RestaurantOverride(
                restaurantId = "node/1",
                phone = "419-555-1234",
                openingHours = "Mo-Fr 09:00-21:00",
                isClosed = false,
                updatedAt = 500L
            )
        )
        sourceDaos.manualInsightDao.upsertManualInsight(
            ManualInsight(
                restaurantId = "node/1",
                summary = "Cozy corner diner",
                recommendedDishes = "Perch basket",
                priceTier = "$$",
                parkingNotes = "Lot in rear",
                goodToKnow = "Cash & card",
                updatedAt = 600L
            )
        )
        sourceDaos.restaurantPhotoDao.insertPhoto(
            RestaurantPhoto(
                restaurantId = "node/1",
                relativePath = "node_1_photo.jpg",
                category = "Food",
                caption = "Lunch special",
                addedAt = 700L
            )
        )
        sourceDaos.visitLogDao.upsertVisitLog(
            VisitLog(
                restaurantId = "node/1",
                visitedAt = 800L,
                rating = 5,
                notes = "Fast service",
                orderedDishes = "Reuben"
            )
        )

        val collected = BackupManager.collectBackup(context, sourceDaos)
        val jsonText = BackupManager.encodeBackupJson(collected)
        val decoded = BackupManager.decodeBackupJson(jsonText)

        val targetPrefs = InMemorySharedPreferences()
        val targetContext = TestContext(targetPrefs)
        val targetDaos = createDaoBundle()

        BackupManager.applyBackup(targetContext, targetDaos, decoded)
        // Second apply should be idempotent for photos, visit logs, and dishes
        BackupManager.applyBackup(targetContext, targetDaos, decoded)

        val restored = BackupManager.collectBackup(targetContext, targetDaos)
        assertEquals(6, restored.version)
        assertEquals(3, restored.selectedAnchorIndex)
        assertEquals(1, restored.favorites.size)
        assertEquals("Try \"special\" menu", restored.wishlist.first().notes)
        assertEquals(1, restored.dishWishlist.size)
        assertEquals(1, restored.favoriteDishes.size)
        assertEquals("419-555-1234", restored.overrides.first().phone)
        assertEquals("Cozy corner diner", restored.manualInsights.first().summary)
        assertEquals(1, restored.photos.size)
        assertEquals("node_1_photo.jpg", restored.photos.first().relativePath)
        assertEquals(1, restored.visitLogs.size)
        assertEquals(5, restored.visitLogs.first().rating)
    }

    @Test
    fun legacyV5BackupJsonRestoresCleanly() = runBlocking {
        val v5Json = """
            {
              "selectedAnchorIndex": 2,
              "favorites": [
                {"restaurantId": "node/101", "addedAt": 1700000000000}
              ],
              "wishlist": [
                {"restaurantId": "way/202", "notes": "Patio looks nice", "priority": "High", "addedAt": 1700000001000}
              ],
              "dishWishlist": [
                {"restaurantId": "way/202", "dishName": "Walleye", "notes": "Blackened", "addedAt": 1700000002000}
              ],
              "favoriteDishes": [
                {"restaurantId": "node/101", "dishName": "Smashburger", "notes": "Double", "addedAt": 1700000003000}
              ]
            }
        """.trimIndent()

        val decoded = BackupManager.decodeBackupJson(v5Json)
        val prefs = InMemorySharedPreferences()
        val context = TestContext(prefs)
        val daos = createDaoBundle()

        BackupManager.applyBackup(context, daos, decoded)

        val restored = BackupManager.collectBackup(context, daos)
        assertEquals(2, restored.selectedAnchorIndex)
        assertEquals(listOf(FavoriteBackupDto("node/101", 1700000000000L)), restored.favorites)
        assertEquals("Patio looks nice", restored.wishlist.first().notes)
        assertEquals("Walleye", restored.dishWishlist.first().dishName)
        assertEquals("Smashburger", restored.favoriteDishes.first().dishName)
        assertTrue(restored.overrides.isEmpty())
        assertTrue(restored.manualInsights.isEmpty())
        assertTrue(restored.photos.isEmpty())
        assertTrue(restored.visitLogs.isEmpty())
    }
}

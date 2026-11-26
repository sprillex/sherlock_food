package com.sprillex.restaurantfinder.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class FavoriteBackupDto(
    val restaurantId: String,
    val addedAt: Long = 0L
)

@Serializable
data class WishlistBackupDto(
    val restaurantId: String,
    val notes: String = "",
    val priority: String = "Medium",
    val addedAt: Long = 0L
)

@Serializable
data class DishWishlistBackupDto(
    val restaurantId: String,
    val dishName: String,
    val notes: String = "",
    val addedAt: Long = 0L
)

@Serializable
data class FavoriteDishBackupDto(
    val restaurantId: String,
    val dishName: String,
    val notes: String = "",
    val addedAt: Long = 0L
)

@Serializable
data class RestaurantOverrideBackupDto(
    val restaurantId: String,
    val name: String? = null,
    val amenity: String? = null,
    val cuisine: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val openingHours: String? = null,
    val takeaway: String? = null,
    val delivery: String? = null,
    val outdoorSeating: String? = null,
    val driveThrough: String? = null,
    val wheelchair: String? = null,
    val dietVegetarian: String? = null,
    val dietVegan: String? = null,
    val dietGlutenFree: String? = null,
    val isClosed: Boolean? = null,
    val updatedAt: Long = 0L
)

@Serializable
data class ManualInsightBackupDto(
    val restaurantId: String,
    val summary: String = "",
    val recommendedDishes: String = "",
    val priceTier: String = "",
    val parkingNotes: String = "",
    val goodToKnow: String = "",
    val updatedAt: Long = 0L
)

@Serializable
data class RestaurantPhotoBackupDto(
    val restaurantId: String,
    val relativePath: String,
    val category: String = "Food",
    val caption: String = "",
    val addedAt: Long = 0L
)

@Serializable
data class VisitLogBackupDto(
    val restaurantId: String,
    val visitedAt: Long = 0L,
    val rating: Int = 5,
    val notes: String = "",
    val orderedDishes: String = ""
)

@Serializable
data class CustomRestaurantBackupDto(
    val id: String,
    val name: String,
    val amenity: String = "restaurant",
    val cuisine: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val postcode: String? = null,
    val city: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val openingHours: String? = null,
    val takeaway: String? = null,
    val delivery: String? = null,
    val outdoorSeating: String? = null,
    val driveThrough: String? = null,
    val wheelchair: String? = null,
    val dietVegetarian: String? = null,
    val dietVegan: String? = null,
    val dietGlutenFree: String? = null,
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = 0L
)

@Serializable
data class UserDataBackup(
    val version: Int = 7,
    val selectedAnchorIndex: Int = 0,
    val favorites: List<FavoriteBackupDto> = emptyList(),
    val wishlist: List<WishlistBackupDto> = emptyList(),
    val dishWishlist: List<DishWishlistBackupDto> = emptyList(),
    val favoriteDishes: List<FavoriteDishBackupDto> = emptyList(),
    val overrides: List<RestaurantOverrideBackupDto> = emptyList(),
    val manualInsights: List<ManualInsightBackupDto> = emptyList(),
    val photos: List<RestaurantPhotoBackupDto> = emptyList(),
    val visitLogs: List<VisitLogBackupDto> = emptyList(),
    val customRestaurants: List<CustomRestaurantBackupDto> = emptyList()
)

data class BackupDaos(
    val favoriteDao: FavoriteDao,
    val wishlistDao: WishlistDao,
    val dishWishlistDao: DishWishlistDao,
    val favoriteDishDao: FavoriteDishDao,
    val restaurantOverrideDao: RestaurantOverrideDao,
    val manualInsightDao: ManualInsightDao,
    val restaurantPhotoDao: RestaurantPhotoDao,
    val visitLogDao: VisitLogDao,
    val customRestaurantDao: CustomRestaurantDao
)

object BackupManager {
    const val BACKUP_JSON_FILENAME = "user_data_backup.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun daosFrom(userDatabase: UserDatabase): BackupDaos {
        return BackupDaos(
            favoriteDao = userDatabase.favoriteDao(),
            wishlistDao = userDatabase.wishlistDao(),
            dishWishlistDao = userDatabase.dishWishlistDao(),
            favoriteDishDao = userDatabase.favoriteDishDao(),
            restaurantOverrideDao = userDatabase.restaurantOverrideDao(),
            manualInsightDao = userDatabase.manualInsightDao(),
            restaurantPhotoDao = userDatabase.restaurantPhotoDao(),
            visitLogDao = userDatabase.visitLogDao(),
            customRestaurantDao = userDatabase.customRestaurantDao()
        )
    }

    fun encodeBackupJson(backup: UserDataBackup): String {
        return json.encodeToString(backup)
    }

    fun decodeBackupJson(content: String): UserDataBackup {
        return try {
            json.decodeFromString<UserDataBackup>(content)
        } catch (_: SerializationException) {
            parseLegacyV5Backup(content)
        } catch (_: IllegalArgumentException) {
            parseLegacyV5Backup(content)
        }
    }

    internal fun parseLegacyV5Backup(content: String): UserDataBackup {
        val anchorIndex = "\"selectedAnchorIndex\":\\s*(\\d+)"
            .toRegex()
            .find(content)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull() ?: 0

        val favorites = mutableListOf<FavoriteBackupDto>()
        val favRegex = "\\{\"restaurantId\":\\s*\"?(.*?)\"?,\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()
        for (match in favRegex.findAll(content)) {
            val resId = match.groupValues[1]
            val addedAt = match.groupValues[2].toLongOrNull()
            if (resId.isNotBlank() && addedAt != null) {
                favorites.add(FavoriteBackupDto(restaurantId = resId, addedAt = addedAt))
            }
        }

        val wishlist = mutableListOf<WishlistBackupDto>()
        val wishRegex = "\\{\"restaurantId\":\\s*\"?(.*?)\"?,\\s*\"notes\":\\s*\"(.*?)\",\\s*\"priority\":\\s*\"(.*?)\",\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()
        for (match in wishRegex.findAll(content)) {
            val resId = match.groupValues[1]
            val notes = match.groupValues[2]
            val priority = match.groupValues[3]
            val addedAt = match.groupValues[4].toLongOrNull()
            if (resId.isNotBlank() && addedAt != null) {
                wishlist.add(
                    WishlistBackupDto(
                        restaurantId = resId,
                        notes = notes,
                        priority = priority,
                        addedAt = addedAt
                    )
                )
            }
        }

        val itemRegex = "\\{\"restaurantId\":\\s*\"?(.*?)\"?,\\s*\"dishName\":\\s*\"(.*?)\",\\s*\"notes\":\\s*\"(.*?)\",\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()

        val dishWishlist = mutableListOf<DishWishlistBackupDto>()
        if (content.contains("\"dishWishlist\"")) {
            val dishSection = content.substringAfter("\"dishWishlist\"").substringBefore("\"favoriteDishes\"")
            for (match in itemRegex.findAll(dishSection)) {
                val resId = match.groupValues[1]
                val dishName = match.groupValues[2]
                val notes = match.groupValues[3]
                val addedAt = match.groupValues[4].toLongOrNull()
                if (resId.isNotBlank() && addedAt != null) {
                    dishWishlist.add(
                        DishWishlistBackupDto(
                            restaurantId = resId,
                            dishName = dishName,
                            notes = notes,
                            addedAt = addedAt
                        )
                    )
                }
            }
        }

        val favoriteDishes = mutableListOf<FavoriteDishBackupDto>()
        if (content.contains("\"favoriteDishes\"")) {
            val favDishSection = content.substringAfter("\"favoriteDishes\"")
            for (match in itemRegex.findAll(favDishSection)) {
                val resId = match.groupValues[1]
                val dishName = match.groupValues[2]
                val notes = match.groupValues[3]
                val addedAt = match.groupValues[4].toLongOrNull()
                if (resId.isNotBlank() && addedAt != null) {
                    favoriteDishes.add(
                        FavoriteDishBackupDto(
                            restaurantId = resId,
                            dishName = dishName,
                            notes = notes,
                            addedAt = addedAt
                        )
                    )
                }
            }
        }

        return UserDataBackup(
            version = 5,
            selectedAnchorIndex = anchorIndex,
            favorites = favorites,
            wishlist = wishlist,
            dishWishlist = dishWishlist,
            favoriteDishes = favoriteDishes
        )
    }

    suspend fun collectBackup(context: Context, daos: BackupDaos): UserDataBackup {
        val favorites = daos.favoriteDao.getAllFavorites().first()
        val wishlist = daos.wishlistDao.getAllWishlistItems().first()
        val dishes = daos.dishWishlistDao.getAllDishWishlistItems().first()
        val favoriteDishes = daos.favoriteDishDao.getAllFavoriteDishes().first()
        val overrides = daos.restaurantOverrideDao.getAllOverrides().first()
        val manualInsights = daos.manualInsightDao.getAllManualInsights().first()
        val photos = daos.restaurantPhotoDao.getAllPhotos().first()
        val visitLogs = daos.visitLogDao.getAllVisitLogs().first()
        val customRestaurants = daos.customRestaurantDao.getAllCustomRestaurants().first()
        val selectedAnchorIndex = PreferenceManager(context).getSelectedAnchorIndex()

        return UserDataBackup(
            version = 7,
            selectedAnchorIndex = selectedAnchorIndex,
            favorites = favorites.map { FavoriteBackupDto(it.restaurantId, it.addedAt) },
            wishlist = wishlist.map { WishlistBackupDto(it.restaurantId, it.notes, it.priority, it.addedAt) },
            dishWishlist = dishes.map { DishWishlistBackupDto(it.restaurantId, it.dishName, it.notes, it.addedAt) },
            favoriteDishes = favoriteDishes.map { FavoriteDishBackupDto(it.restaurantId, it.dishName, it.notes, it.addedAt) },
            overrides = overrides.map {
                RestaurantOverrideBackupDto(
                    restaurantId = it.restaurantId,
                    name = it.name,
                    amenity = it.amenity,
                    cuisine = it.cuisine,
                    street = it.street,
                    housenumber = it.housenumber,
                    postcode = it.postcode,
                    city = it.city,
                    phone = it.phone,
                    website = it.website,
                    openingHours = it.openingHours,
                    takeaway = it.takeaway,
                    delivery = it.delivery,
                    outdoorSeating = it.outdoorSeating,
                    driveThrough = it.driveThrough,
                    wheelchair = it.wheelchair,
                    dietVegetarian = it.dietVegetarian,
                    dietVegan = it.dietVegan,
                    dietGlutenFree = it.dietGlutenFree,
                    isClosed = it.isClosed,
                    updatedAt = it.updatedAt
                )
            },
            manualInsights = manualInsights.map {
                ManualInsightBackupDto(
                    restaurantId = it.restaurantId,
                    summary = it.summary,
                    recommendedDishes = it.recommendedDishes,
                    priceTier = it.priceTier,
                    parkingNotes = it.parkingNotes,
                    goodToKnow = it.goodToKnow,
                    updatedAt = it.updatedAt
                )
            },
            photos = photos.map {
                RestaurantPhotoBackupDto(
                    restaurantId = it.restaurantId,
                    relativePath = it.relativePath,
                    category = it.category,
                    caption = it.caption,
                    addedAt = it.addedAt
                )
            },
            visitLogs = visitLogs.map {
                VisitLogBackupDto(
                    restaurantId = it.restaurantId,
                    visitedAt = it.visitedAt,
                    rating = it.rating,
                    notes = it.notes,
                    orderedDishes = it.orderedDishes
                )
            },
            customRestaurants = customRestaurants.map {
                CustomRestaurantBackupDto(
                    id = it.id,
                    name = it.name,
                    amenity = it.amenity,
                    cuisine = it.cuisine,
                    street = it.street,
                    housenumber = it.housenumber,
                    postcode = it.postcode,
                    city = it.city,
                    phone = it.phone,
                    website = it.website,
                    openingHours = it.openingHours,
                    takeaway = it.takeaway,
                    delivery = it.delivery,
                    outdoorSeating = it.outdoorSeating,
                    driveThrough = it.driveThrough,
                    wheelchair = it.wheelchair,
                    dietVegetarian = it.dietVegetarian,
                    dietVegan = it.dietVegan,
                    dietGlutenFree = it.dietGlutenFree,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    createdAt = it.createdAt
                )
            }
        )
    }

    suspend fun applyBackup(context: Context, daos: BackupDaos, backup: UserDataBackup) {
        PreferenceManager(context).setSelectedAnchorIndex(backup.selectedAnchorIndex)

        for (fav in backup.favorites) {
            if (fav.restaurantId.isNotBlank()) {
                daos.favoriteDao.addFavorite(
                    Favorite(restaurantId = fav.restaurantId, addedAt = fav.addedAt)
                )
            }
        }

        for (item in backup.wishlist) {
            if (item.restaurantId.isNotBlank()) {
                daos.wishlistDao.addOrUpdateWishlist(
                    Wishlist(
                        restaurantId = item.restaurantId,
                        notes = item.notes,
                        priority = item.priority,
                        addedAt = item.addedAt
                    )
                )
            }
        }

        val existingDishes = daos.dishWishlistDao.getAllDishWishlistItems().first()
        for (dish in backup.dishWishlist) {
            if (dish.restaurantId.isNotBlank() && existingDishes.none {
                    it.restaurantId == dish.restaurantId &&
                        it.dishName == dish.dishName &&
                        it.addedAt == dish.addedAt
                }
            ) {
                daos.dishWishlistDao.addOrUpdateDish(
                    DishWishlist(
                        restaurantId = dish.restaurantId,
                        dishName = dish.dishName,
                        notes = dish.notes,
                        addedAt = dish.addedAt
                    )
                )
            }
        }

        val existingFavDishes = daos.favoriteDishDao.getAllFavoriteDishes().first()
        for (favDish in backup.favoriteDishes) {
            if (favDish.restaurantId.isNotBlank() && existingFavDishes.none {
                    it.restaurantId == favDish.restaurantId &&
                        it.dishName == favDish.dishName &&
                        it.addedAt == favDish.addedAt
                }
            ) {
                daos.favoriteDishDao.addOrUpdateFavoriteDish(
                    FavoriteDish(
                        restaurantId = favDish.restaurantId,
                        dishName = favDish.dishName,
                        notes = favDish.notes,
                        addedAt = favDish.addedAt
                    )
                )
            }
        }

        for (ov in backup.overrides) {
            if (ov.restaurantId.isNotBlank()) {
                daos.restaurantOverrideDao.upsertOverride(
                    RestaurantOverride(
                        restaurantId = ov.restaurantId,
                        name = ov.name,
                        amenity = ov.amenity,
                        cuisine = ov.cuisine,
                        street = ov.street,
                        housenumber = ov.housenumber,
                        postcode = ov.postcode,
                        city = ov.city,
                        phone = ov.phone,
                        website = ov.website,
                        openingHours = ov.openingHours,
                        takeaway = ov.takeaway,
                        delivery = ov.delivery,
                        outdoorSeating = ov.outdoorSeating,
                        driveThrough = ov.driveThrough,
                        wheelchair = ov.wheelchair,
                        dietVegetarian = ov.dietVegetarian,
                        dietVegan = ov.dietVegan,
                        dietGlutenFree = ov.dietGlutenFree,
                        isClosed = ov.isClosed,
                        updatedAt = ov.updatedAt
                    )
                )
            }
        }

        for (insight in backup.manualInsights) {
            if (insight.restaurantId.isNotBlank()) {
                daos.manualInsightDao.upsertManualInsight(
                    ManualInsight(
                        restaurantId = insight.restaurantId,
                        summary = insight.summary,
                        recommendedDishes = insight.recommendedDishes,
                        priceTier = insight.priceTier,
                        parkingNotes = insight.parkingNotes,
                        goodToKnow = insight.goodToKnow,
                        updatedAt = insight.updatedAt
                    )
                )
            }
        }

        for (photo in backup.photos) {
            if (photo.restaurantId.isNotBlank() && photo.relativePath.isNotBlank()) {
                val existing = daos.restaurantPhotoDao.getPhotosListForRestaurant(photo.restaurantId)
                if (existing.none { it.relativePath == photo.relativePath }) {
                    daos.restaurantPhotoDao.insertPhoto(
                        RestaurantPhoto(
                            restaurantId = photo.restaurantId,
                            relativePath = photo.relativePath,
                            category = photo.category,
                            caption = photo.caption,
                            addedAt = photo.addedAt
                        )
                    )
                }
            }
        }

        for (visit in backup.visitLogs) {
            if (visit.restaurantId.isNotBlank()) {
                val existing = daos.visitLogDao.getVisitLogsListForRestaurant(visit.restaurantId)
                val clampedRating = visit.rating.coerceIn(1, 5)
                if (existing.none {
                        it.visitedAt == visit.visitedAt &&
                            it.rating == clampedRating &&
                            it.notes == visit.notes &&
                            it.orderedDishes == visit.orderedDishes
                    }
                ) {
                    daos.visitLogDao.upsertVisitLog(
                        VisitLog(
                            restaurantId = visit.restaurantId,
                            visitedAt = visit.visitedAt,
                            rating = clampedRating,
                            notes = visit.notes,
                            orderedDishes = visit.orderedDishes
                        )
                    )
                }
            }
        }

        for (custom in backup.customRestaurants) {
            if (custom.id.isNotBlank() && custom.name.isNotBlank()) {
                daos.customRestaurantDao.upsertCustomRestaurant(
                    CustomRestaurant(
                        id = custom.id,
                        name = custom.name,
                        amenity = custom.amenity.ifBlank { "restaurant" },
                        cuisine = custom.cuisine,
                        street = custom.street,
                        housenumber = custom.housenumber,
                        postcode = custom.postcode,
                        city = custom.city,
                        phone = custom.phone,
                        website = custom.website,
                        openingHours = custom.openingHours,
                        takeaway = custom.takeaway,
                        delivery = custom.delivery,
                        outdoorSeating = custom.outdoorSeating,
                        driveThrough = custom.driveThrough,
                        wheelchair = custom.wheelchair,
                        dietVegetarian = custom.dietVegetarian,
                        dietVegan = custom.dietVegan,
                        dietGlutenFree = custom.dietGlutenFree,
                        latitude = custom.latitude,
                        longitude = custom.longitude,
                        createdAt = custom.createdAt
                    )
                )
            }
        }
    }

    suspend fun exportUserDataToJson(context: Context, userDatabase: UserDatabase): String {
        val backup = collectBackup(context, daosFrom(userDatabase))
        return encodeBackupJson(backup)
    }

    suspend fun exportUserData(context: Context, userDatabase: UserDatabase): String {
        val jsonContent = exportUserDataToJson(context, userDatabase)
        val backupFile = File(context.filesDir, BACKUP_JSON_FILENAME)
        backupFile.writeText(jsonContent)
        return backupFile.absolutePath
    }

    suspend fun restoreUserDataFromJson(
        context: Context,
        userDatabase: UserDatabase,
        content: String
    ): Boolean {
        val backup = decodeBackupJson(content)
        applyBackup(context, daosFrom(userDatabase), backup)
        return true
    }

    suspend fun restoreUserData(
        context: Context,
        userDatabase: UserDatabase,
        backupFile: File
    ): Boolean {
        if (!backupFile.exists()) return false
        val content = backupFile.readText()
        return restoreUserDataFromJson(context, userDatabase, content)
    }

    suspend fun exportBackupZip(
        context: Context,
        userDatabase: UserDatabase,
        outputStream: OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        val jsonContent = exportUserDataToJson(context, userDatabase)
        val photosDir = PhotoStorageManager.getPhotosDirectory(context)
        ZipOutputStream(outputStream.buffered()).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(BACKUP_JSON_FILENAME))
            zipOut.write(jsonContent.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            if (photosDir.exists() && photosDir.isDirectory) {
                photosDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val rel = file.relativeTo(photosDir).path.replace(File.separatorChar, '/')
                        zipOut.putNextEntry(ZipEntry("${PhotoStorageManager.PHOTOS_DIR_NAME}/$rel"))
                        file.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
            }
        }
        true
    }

    suspend fun exportBackupArchive(
        context: Context,
        userDatabase: UserDatabase,
        destinationUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val out = context.contentResolver.openOutputStream(destinationUri) ?: return@withContext false
            out.use { exportBackupZip(context, userDatabase, it) }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun restoreBackupZip(
        context: Context,
        userDatabase: UserDatabase,
        inputStream: InputStream
    ): Boolean = withContext(Dispatchers.IO) {
        var jsonContent: String? = null
        val photosDir = PhotoStorageManager.getPhotosDirectory(context)
        val baseCanonical = photosDir.canonicalPath

        ZipInputStream(inputStream.buffered()).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    if (entry.name == BACKUP_JSON_FILENAME) {
                        val buffer = ByteArrayOutputStream()
                        zipIn.copyTo(buffer)
                        jsonContent = buffer.toString(Charsets.UTF_8.name())
                    } else if (entry.name.startsWith("${PhotoStorageManager.PHOTOS_DIR_NAME}/")) {
                        val relPath = entry.name.removePrefix("${PhotoStorageManager.PHOTOS_DIR_NAME}/")
                        if (relPath.isNotBlank()) {
                            val targetFile = File(photosDir, relPath)
                            val targetCanonical = targetFile.canonicalPath
                            if (targetCanonical.startsWith(baseCanonical + File.separator)) {
                                targetFile.parentFile?.mkdirs()
                                targetFile.outputStream().use { out ->
                                    zipIn.copyTo(out)
                                }
                            }
                        }
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        val parsedJson = jsonContent ?: return@withContext false
        restoreUserDataFromJson(context, userDatabase, parsedJson)
    }

    suspend fun restoreBackupArchive(
        context: Context,
        userDatabase: UserDatabase,
        sourceUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(sourceUri) ?: return@withContext false
            input.use { restoreBackupZip(context, userDatabase, it) }
        } catch (_: Exception) {
            false
        }
    }
}

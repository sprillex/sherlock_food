package com.sprillex.restaurantfinder.data

import android.content.Context
import kotlinx.coroutines.flow.first
import java.io.File

object BackupManager {
    suspend fun exportUserData(context: Context, userDatabase: UserDatabase): String {
        val favorites = userDatabase.favoriteDao().getAllFavorites().first()
        val wishlist = userDatabase.wishlistDao().getAllWishlistItems().first()
        val dishes = userDatabase.dishWishlistDao().getAllDishWishlistItems().first()
        val favoriteDishes = userDatabase.favoriteDishDao().getAllFavoriteDishes().first()
        val preferenceManager = PreferenceManager(context)
        val selectedAnchorIndex = preferenceManager.getSelectedAnchorIndex()

        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"selectedAnchorIndex\": $selectedAnchorIndex,\n")
        json.append("  \"favorites\": [\n")
        favorites.forEachIndexed { index, fav ->
            val safeResId = fav.restaurantId.replace("\"", "\\\"")
            json.append("    {\"restaurantId\": \"$safeResId\", \"addedAt\": ${fav.addedAt}}")
            if (index < favorites.size - 1) json.append(",")
            json.append("\n")
        }
        json.append("  ],\n")
        json.append("  \"wishlist\": [\n")
        wishlist.forEachIndexed { index, item ->
            val safeResId = item.restaurantId.replace("\"", "\\\"")
            val safeNotes = item.notes.replace("\"", "\\\"")
            json.append("    {\"restaurantId\": \"$safeResId\", \"notes\": \"$safeNotes\", \"priority\": \"${item.priority}\", \"addedAt\": ${item.addedAt}}")
            if (index < wishlist.size - 1) json.append(",")
            json.append("\n")
        }
        json.append("  ],\n")
        json.append("  \"dishWishlist\": [\n")
        dishes.forEachIndexed { index, dish ->
            val safeResId = dish.restaurantId.replace("\"", "\\\"")
            val safeName = dish.dishName.replace("\"", "\\\"")
            val safeNotes = dish.notes.replace("\"", "\\\"")
            json.append("    {\"restaurantId\": \"$safeResId\", \"dishName\": \"$safeName\", \"notes\": \"$safeNotes\", \"addedAt\": ${dish.addedAt}}")
            if (index < dishes.size - 1) json.append(",")
            json.append("\n")
        }
        json.append("  ],\n")
        json.append("  \"favoriteDishes\": [\n")
        favoriteDishes.forEachIndexed { index, favDish ->
            val safeResId = favDish.restaurantId.replace("\"", "\\\"")
            val safeName = favDish.dishName.replace("\"", "\\\"")
            val safeNotes = favDish.notes.replace("\"", "\\\"")
            json.append("    {\"restaurantId\": \"$safeResId\", \"dishName\": \"$safeName\", \"notes\": \"$safeNotes\", \"addedAt\": ${favDish.addedAt}}")
            if (index < favoriteDishes.size - 1) json.append(",")
            json.append("\n")
        }
        json.append("  ]\n")
        json.append("}\n")

        val backupFile = File(context.filesDir, "user_data_backup.json")
        backupFile.writeText(json.toString())
        return backupFile.absolutePath
    }

    suspend fun restoreUserData(context: Context, userDatabase: UserDatabase, backupFile: File): Boolean {
        if (!backupFile.exists()) return false
        val content = backupFile.readText()

        val anchorIndexRegex = "\"selectedAnchorIndex\":\\s*(\\d+)".toRegex()
        anchorIndexRegex.find(content)?.groupValues?.get(1)?.toIntOrNull()?.let { index ->
            PreferenceManager(context).setSelectedAnchorIndex(index)
        }

        val favRegex = "\\{\"restaurantId\":\\s*\"?(.*?)\"?,\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()
        val matches = favRegex.findAll(content)
        for (match in matches) {
            val resId = match.groupValues[1]
            val addedAt = match.groupValues[2].toLongOrNull()
            if (resId.isNotBlank() && addedAt != null) {
                userDatabase.favoriteDao().addFavorite(Favorite(restaurantId = resId, addedAt = addedAt))
            }
        }

        val wishRegex = "\\{\"restaurantId\":\\s*\"?(.*?)\"?,\\s*\"notes\":\\s*\"(.*?)\",\\s*\"priority\":\\s*\"(.*?)\",\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()
        val wishMatches = wishRegex.findAll(content)
        for (match in wishMatches) {
            val resId = match.groupValues[1]
            val notes = match.groupValues[2]
            val priority = match.groupValues[3]
            val addedAt = match.groupValues[4].toLongOrNull()
            if (resId.isNotBlank() && addedAt != null) {
                userDatabase.wishlistDao().addOrUpdateWishlist(
                    Wishlist(
                        restaurantId = resId,
                        notes = notes,
                        priority = priority,
                        addedAt = addedAt
                    )
                )
            }
        }

        val itemRegex = "\\{\"restaurantId\":\\s*\"?(.*?)\"?,\\s*\"dishName\":\\s*\"(.*?)\",\\s*\"notes\":\\s*\"(.*?)\",\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()

        if (content.contains("\"dishWishlist\"")) {
            val dishSection = content.substringAfter("\"dishWishlist\"").substringBefore("\"favoriteDishes\"")
            val dishMatches = itemRegex.findAll(dishSection)
            for (match in dishMatches) {
                val resId = match.groupValues[1]
                val dishName = match.groupValues[2]
                val notes = match.groupValues[3]
                val addedAt = match.groupValues[4].toLongOrNull()
                if (resId.isNotBlank() && addedAt != null) {
                    userDatabase.dishWishlistDao().addOrUpdateDish(
                        DishWishlist(
                            restaurantId = resId,
                            dishName = dishName,
                            notes = notes,
                            addedAt = addedAt
                        )
                    )
                }
            }
        }

        if (content.contains("\"favoriteDishes\"")) {
            val favDishSection = content.substringAfter("\"favoriteDishes\"")
            val favDishMatches = itemRegex.findAll(favDishSection)
            for (match in favDishMatches) {
                val resId = match.groupValues[1]
                val dishName = match.groupValues[2]
                val notes = match.groupValues[3]
                val addedAt = match.groupValues[4].toLongOrNull()
                if (resId.isNotBlank() && addedAt != null) {
                    userDatabase.favoriteDishDao().addOrUpdateFavoriteDish(
                        FavoriteDish(
                            restaurantId = resId,
                            dishName = dishName,
                            notes = notes,
                            addedAt = addedAt
                        )
                    )
                }
            }
        }
        return true
    }
}

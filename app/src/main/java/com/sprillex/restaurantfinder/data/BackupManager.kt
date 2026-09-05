package com.sprillex.restaurantfinder.data

import android.content.Context
import kotlinx.coroutines.flow.first
import java.io.File

object BackupManager {
    suspend fun exportUserData(context: Context, userDatabase: UserDatabase): String {
        val favorites = userDatabase.favoriteDao().getAllFavorites().first()
        val wishlist = userDatabase.wishlistDao().getAllWishlistItems().first()
        val dishes = userDatabase.dishWishlistDao().getAllDishWishlistItems().first()
        val preferenceManager = PreferenceManager(context)
        val selectedAnchorIndex = preferenceManager.getSelectedAnchorIndex()

        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"selectedAnchorIndex\": $selectedAnchorIndex,\n")
        json.append("  \"favorites\": [\n")
        favorites.forEachIndexed { index, fav ->
            json.append("    {\"restaurantId\": ${fav.restaurantId}, \"addedAt\": ${fav.addedAt}}")
            if (index < favorites.size - 1) json.append(",")
            json.append("\n")
        }
        json.append("  ],\n")
        json.append("  \"wishlist\": [\n")
        wishlist.forEachIndexed { index, item ->
            val safeNotes = item.notes.replace("\"", "\\\"")
            json.append("    {\"restaurantId\": ${item.restaurantId}, \"notes\": \"$safeNotes\", \"priority\": \"${item.priority}\", \"addedAt\": ${item.addedAt}}")
            if (index < wishlist.size - 1) json.append(",")
            json.append("\n")
        }
        json.append("  ],\n")
        json.append("  \"dishWishlist\": [\n")
        dishes.forEachIndexed { index, dish ->
            val safeName = dish.dishName.replace("\"", "\\\"")
            val safeNotes = dish.notes.replace("\"", "\\\"")
            json.append("    {\"restaurantId\": ${dish.restaurantId}, \"dishName\": \"$safeName\", \"notes\": \"$safeNotes\", \"addedAt\": ${dish.addedAt}}")
            if (index < dishes.size - 1) json.append(",")
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

        val favRegex = "\\{\"restaurantId\":\\s*(\\d+),\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()
        val matches = favRegex.findAll(content)
        for (match in matches) {
            val resId = match.groupValues[1].toLongOrNull()
            val addedAt = match.groupValues[2].toLongOrNull()
            if (resId != null && addedAt != null) {
                userDatabase.favoriteDao().addFavorite(Favorite(restaurantId = resId, addedAt = addedAt))
            }
        }

        val wishRegex = "\\{\"restaurantId\":\\s*(\\d+),\\s*\"notes\":\\s*\"(.*?)\",\\s*\"priority\":\\s*\"(.*?)\",\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()
        val wishMatches = wishRegex.findAll(content)
        for (match in wishMatches) {
            val resId = match.groupValues[1].toLongOrNull()
            val notes = match.groupValues[2]
            val priority = match.groupValues[3]
            val addedAt = match.groupValues[4].toLongOrNull()
            if (resId != null && addedAt != null) {
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

        val dishRegex = "\\{\"restaurantId\":\\s*(\\d+),\\s*\"dishName\":\\s*\"(.*?)\",\\s*\"notes\":\\s*\"(.*?)\",\\s*\"addedAt\":\\s*(\\d+)\\}".toRegex()
        val dishMatches = dishRegex.findAll(content)
        for (match in dishMatches) {
            val resId = match.groupValues[1].toLongOrNull()
            val dishName = match.groupValues[2]
            val notes = match.groupValues[3]
            val addedAt = match.groupValues[4].toLongOrNull()
            if (resId != null && addedAt != null) {
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
        return true
    }
}

package com.sprillex.restaurantfinder.data

import android.content.Context
import kotlinx.coroutines.flow.first
import java.io.File

object BackupManager {
    suspend fun exportUserData(context: Context, userDatabase: UserDatabase): String {
        val favorites = userDatabase.favoriteDao().getAllFavorites().first()
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
        return true
    }
}

package com.sprillex.restaurantfinder.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("restaurant_finder_prefs", Context.MODE_PRIVATE)

    fun getSelectedAnchorIndex(): Int {
        return prefs.getInt(KEY_ANCHOR_INDEX, 0)
    }

    fun setSelectedAnchorIndex(index: Int) {
        prefs.edit().putInt(KEY_ANCHOR_INDEX, index).apply()
    }

    fun getGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }

    fun setGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    companion object {
        private const val KEY_ANCHOR_INDEX = "key_anchor_index"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
    }
}

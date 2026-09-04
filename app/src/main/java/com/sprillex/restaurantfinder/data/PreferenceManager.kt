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

    companion object {
        private const val KEY_ANCHOR_INDEX = "key_anchor_index"
    }
}

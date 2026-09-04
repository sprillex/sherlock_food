package com.sprillex.restaurantfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import com.sprillex.restaurantfinder.data.AppDatabase
import com.sprillex.restaurantfinder.data.PreferenceManager
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.location.LocationManager
import com.sprillex.restaurantfinder.ui.screens.DetailBottomSheet
import com.sprillex.restaurantfinder.ui.screens.MainScreen
import com.sprillex.restaurantfinder.ui.theme.RestaurantFinderTheme
import com.sprillex.restaurantfinder.utils.IntentHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferenceManager = PreferenceManager(this)
        val initialAnchorIndex = preferenceManager.getSelectedAnchorIndex().coerceIn(0, LocationManager.PRESET_ANCHORS.lastIndex)

        setContent {
            RestaurantFinderTheme {
                Surface {
                    val db = remember { AppDatabase.getDatabase(applicationContext) }
                    val restaurantsFlow = remember { db.restaurantDao().getAllRestaurants() }
                    val restaurants by restaurantsFlow.collectAsState(initial = emptyList())

                    var selectedAnchor by remember {
                        mutableStateOf(LocationManager.PRESET_ANCHORS[initialAnchorIndex])
                    }
                    var selectedRestaurantForDetail by remember { mutableStateOf<Restaurant?>(null) }

                    MainScreen(
                        restaurants = restaurants,
                        selectedAnchor = selectedAnchor,
                        onAnchorSelected = { anchor ->
                            selectedAnchor = anchor
                            val newIndex = LocationManager.PRESET_ANCHORS.indexOf(anchor)
                            if (newIndex >= 0) {
                                preferenceManager.setSelectedAnchorIndex(newIndex)
                            }
                        },
                        onRestaurantClick = { selectedRestaurantForDetail = it },
                        onNavigateClick = { IntentHelper.launchNavigation(this, it) },
                        onCallClick = { r -> r.phone?.let { IntentHelper.launchDialer(this, it) } },
                        onWebsiteClick = { r -> r.website?.let { IntentHelper.launchBrowser(this, it) } }
                    )

                    selectedRestaurantForDetail?.let { restaurant ->
                        DetailBottomSheet(
                            restaurant = restaurant,
                            onDismiss = { selectedRestaurantForDetail = null },
                            onNavigateClick = { IntentHelper.launchNavigation(this, restaurant) },
                            onCallClick = { restaurant.phone?.let { IntentHelper.launchDialer(this, it) } },
                            onWebsiteClick = { restaurant.website?.let { IntentHelper.launchBrowser(this, it) } }
                        )
                    }
                }
            }
        }
    }
}

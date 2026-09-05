package com.sprillex.restaurantfinder

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.sprillex.restaurantfinder.data.*
import com.sprillex.restaurantfinder.location.LocationManager
import com.sprillex.restaurantfinder.ui.screens.DetailBottomSheet
import com.sprillex.restaurantfinder.ui.screens.MainScreen
import com.sprillex.restaurantfinder.ui.theme.RestaurantFinderTheme
import com.sprillex.restaurantfinder.utils.IntentHelper
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferenceManager = PreferenceManager(this)
        val initialAnchorIndex = preferenceManager.getSelectedAnchorIndex().coerceIn(0, LocationManager.PRESET_ANCHORS.lastIndex)

        setContent {
            RestaurantFinderTheme {
                Surface {
                    val appDb = remember { AppDatabase.getDatabase(applicationContext) }
                    val userDb = remember { UserDatabase.getDatabase(applicationContext) }

                    val restaurantsFlow = remember { appDb.restaurantDao().getAllRestaurants() }
                    val restaurants by restaurantsFlow.collectAsState(initial = emptyList())

                    val favoritesFlow = remember { userDb.favoriteDao().getAllFavorites() }
                    val favorites by favoritesFlow.collectAsState(initial = emptyList())
                    val favoriteIds = remember(favorites) { favorites.map { it.restaurantId }.toSet() }

                    var selectedAnchor by remember {
                        mutableStateOf(LocationManager.PRESET_ANCHORS[initialAnchorIndex])
                    }
                    var selectedRestaurantForDetail by remember { mutableStateOf<Restaurant?>(null) }

                    val toggleFavorite: (Long) -> Unit = { restaurantId ->
                        lifecycleScope.launch {
                            if (favoriteIds.contains(restaurantId)) {
                                userDb.favoriteDao().removeFavorite(Favorite(restaurantId))
                            } else {
                                userDb.favoriteDao().addFavorite(Favorite(restaurantId))
                            }
                        }
                    }

                    MainScreen(
                        restaurants = restaurants,
                        favoriteIds = favoriteIds,
                        selectedAnchor = selectedAnchor,
                        onAnchorSelected = { anchor ->
                            selectedAnchor = anchor
                            val newIndex = LocationManager.PRESET_ANCHORS.indexOf(anchor)
                            if (newIndex >= 0) {
                                preferenceManager.setSelectedAnchorIndex(newIndex)
                            }
                        },
                        onFavoriteToggle = toggleFavorite,
                        onBackupClick = {
                            lifecycleScope.launch {
                                val path = BackupManager.exportUserData(applicationContext, userDb)
                                Toast.makeText(applicationContext, "Backup saved: $path", Toast.LENGTH_LONG).show()
                            }
                        },
                        onRestoreClick = {
                            lifecycleScope.launch {
                                val backupFile = File(filesDir, "user_data_backup.json")
                                val success = BackupManager.restoreUserData(applicationContext, userDb, backupFile)
                                val msg = if (success) "User data restored successfully" else "No backup file found"
                                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRestaurantClick = { selectedRestaurantForDetail = it },
                        onNavigateClick = { IntentHelper.launchNavigation(this, it) },
                        onCallClick = { r -> r.phone?.let { IntentHelper.launchDialer(this, it) } },
                        onWebsiteClick = { r -> r.website?.let { IntentHelper.launchBrowser(this, it) } },
                        onShareClick = { IntentHelper.shareRestaurant(this, it) }
                    )

                    selectedRestaurantForDetail?.let { restaurant ->
                        DetailBottomSheet(
                            restaurant = restaurant,
                            isFavorite = favoriteIds.contains(restaurant.id),
                            onFavoriteToggle = { toggleFavorite(restaurant.id) },
                            onDismiss = { selectedRestaurantForDetail = null },
                            onNavigateClick = { IntentHelper.launchNavigation(this, restaurant) },
                            onCallClick = { restaurant.phone?.let { IntentHelper.launchDialer(this, it) } },
                            onWebsiteClick = { restaurant.website?.let { IntentHelper.launchBrowser(this, it) } },
                            onShareClick = { IntentHelper.shareRestaurant(this, restaurant) }
                        )
                    }
                }
            }
        }
    }
}

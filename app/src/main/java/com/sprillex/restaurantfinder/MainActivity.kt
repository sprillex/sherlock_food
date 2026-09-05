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
import com.sprillex.restaurantfinder.ui.components.AddDishDialog
import com.sprillex.restaurantfinder.ui.components.AddFavoriteDishDialog
import com.sprillex.restaurantfinder.ui.components.WishlistDialog
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

                    val wishlistFlow = remember { userDb.wishlistDao().getAllWishlistItems() }
                    val wishlistItems by wishlistFlow.collectAsState(initial = emptyList())
                    val wishlistMap = remember(wishlistItems) { wishlistItems.associateBy { it.restaurantId } }

                    val dishWishlistFlow = remember { userDb.dishWishlistDao().getAllDishWishlistItems() }
                    val dishWishlistItems by dishWishlistFlow.collectAsState(initial = emptyList())
                    val dishWishlistMap = remember(dishWishlistItems) { dishWishlistItems.groupBy { it.restaurantId } }

                    val favoriteDishFlow = remember { userDb.favoriteDishDao().getAllFavoriteDishes() }
                    val favoriteDishItems by favoriteDishFlow.collectAsState(initial = emptyList())
                    val favoriteDishMap = remember(favoriteDishItems) { favoriteDishItems.groupBy { it.restaurantId } }

                    var selectedAnchor by remember {
                        mutableStateOf(LocationManager.PRESET_ANCHORS[initialAnchorIndex])
                    }
                    var selectedRestaurantForDetail by remember { mutableStateOf<Restaurant?>(null) }
                    var selectedRestaurantForWishlist by remember { mutableStateOf<Restaurant?>(null) }
                    var selectedRestaurantForAddDish by remember { mutableStateOf<Restaurant?>(null) }
                    var selectedRestaurantForAddFavDish by remember { mutableStateOf<Restaurant?>(null) }

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
                        wishlistMap = wishlistMap,
                        dishWishlistMap = dishWishlistMap,
                        favoriteDishMap = favoriteDishMap,
                        selectedAnchor = selectedAnchor,
                        onAnchorSelected = { anchor ->
                            selectedAnchor = anchor
                            val newIndex = LocationManager.PRESET_ANCHORS.indexOf(anchor)
                            if (newIndex >= 0) {
                                preferenceManager.setSelectedAnchorIndex(newIndex)
                            }
                        },
                        onFavoriteToggle = toggleFavorite,
                        onWishlistClick = { selectedRestaurantForWishlist = it },
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
                            wishlist = wishlistMap[restaurant.id],
                            dishes = dishWishlistMap[restaurant.id] ?: emptyList(),
                            favoriteDishes = favoriteDishMap[restaurant.id] ?: emptyList(),
                            onFavoriteToggle = { toggleFavorite(restaurant.id) },
                            onWishlistClick = {
                                selectedRestaurantForWishlist = restaurant
                            },
                            onAddDishClick = {
                                selectedRestaurantForAddDish = restaurant
                            },
                            onDeleteDishClick = { dish ->
                                lifecycleScope.launch {
                                    userDb.dishWishlistDao().removeDish(dish)
                                }
                            },
                            onAddFavoriteDishClick = {
                                selectedRestaurantForAddFavDish = restaurant
                            },
                            onDeleteFavoriteDishClick = { favDish ->
                                lifecycleScope.launch {
                                    userDb.favoriteDishDao().removeFavoriteDish(favDish)
                                }
                            },
                            onDismiss = { selectedRestaurantForDetail = null },
                            onNavigateClick = { IntentHelper.launchNavigation(this, restaurant) },
                            onCallClick = { restaurant.phone?.let { IntentHelper.launchDialer(this, it) } },
                            onWebsiteClick = { restaurant.website?.let { IntentHelper.launchBrowser(this, it) } },
                            onShareClick = { IntentHelper.shareRestaurant(this, restaurant) }
                        )
                    }

                    selectedRestaurantForAddFavDish?.let { restaurant ->
                        AddFavoriteDishDialog(
                            restaurant = restaurant,
                            onSave = { dishName, notes ->
                                lifecycleScope.launch {
                                    userDb.favoriteDishDao().addOrUpdateFavoriteDish(
                                        FavoriteDish(
                                            restaurantId = restaurant.id,
                                            dishName = dishName,
                                            notes = notes
                                        )
                                    )
                                    selectedRestaurantForAddFavDish = null
                                }
                            },
                            onDismiss = { selectedRestaurantForAddFavDish = null }
                        )
                    }

                    selectedRestaurantForAddDish?.let { restaurant ->
                        AddDishDialog(
                            restaurant = restaurant,
                            onSave = { dishName, notes ->
                                lifecycleScope.launch {
                                    userDb.dishWishlistDao().addOrUpdateDish(
                                        DishWishlist(
                                            restaurantId = restaurant.id,
                                            dishName = dishName,
                                            notes = notes
                                        )
                                    )
                                    selectedRestaurantForAddDish = null
                                }
                            },
                            onDismiss = { selectedRestaurantForAddDish = null }
                        )
                    }

                    selectedRestaurantForWishlist?.let { restaurant ->
                        WishlistDialog(
                            restaurant = restaurant,
                            currentWishlist = wishlistMap[restaurant.id],
                            onSave = { notes, priority ->
                                lifecycleScope.launch {
                                    userDb.wishlistDao().addOrUpdateWishlist(
                                        Wishlist(
                                            restaurantId = restaurant.id,
                                            notes = notes,
                                            priority = priority
                                        )
                                    )
                                    selectedRestaurantForWishlist = null
                                }
                            },
                            onDelete = {
                                lifecycleScope.launch {
                                    userDb.wishlistDao().removeWishlistByRestaurantId(restaurant.id)
                                    selectedRestaurantForWishlist = null
                                }
                            },
                            onDismiss = { selectedRestaurantForWishlist = null }
                        )
                    }
                }
            }
        }
    }
}

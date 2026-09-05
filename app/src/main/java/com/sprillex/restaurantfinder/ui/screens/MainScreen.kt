package com.sprillex.restaurantfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.DishWishlist
import com.sprillex.restaurantfinder.data.FavoriteDish
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.Wishlist
import com.sprillex.restaurantfinder.location.AnchorLocation
import com.sprillex.restaurantfinder.location.DistanceCalculator
import com.sprillex.restaurantfinder.location.LocationManager
import com.sprillex.restaurantfinder.ui.components.RestaurantCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    restaurants: List<Restaurant>,
    favoriteIds: Set<Long>,
    wishlistMap: Map<Long, Wishlist>,
    dishWishlistMap: Map<Long, List<DishWishlist>>,
    favoriteDishMap: Map<Long, List<FavoriteDish>>,
    selectedAnchor: AnchorLocation,
    onAnchorSelected: (AnchorLocation) -> Unit,
    onFavoriteToggle: (Long) -> Unit,
    onWishlistClick: (Restaurant) -> Unit,
    onBackupClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onRestaurantClick: (Restaurant) -> Unit,
    onNavigateClick: (Restaurant) -> Unit,
    onCallClick: (Restaurant) -> Unit,
    onWebsiteClick: (Restaurant) -> Unit,
    onShareClick: (Restaurant) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAmenity by remember { mutableStateOf<String?>(null) }
    var anchorMenuExpanded by remember { mutableStateOf(false) }

    val categories = listOf("All", "Favorites", "Wishlist", "restaurant", "fast_food", "cafe", "bar", "pub")

    val filteredRestaurants = remember(restaurants, favoriteIds, wishlistMap, searchQuery, selectedAmenity, selectedAnchor) {
        restaurants.filter { r ->
            val matchesQuery = searchQuery.isEmpty() ||
                    r.name.contains(searchQuery, ignoreCase = true) ||
                    (r.cuisine?.contains(searchQuery, ignoreCase = true) == true) ||
                    (r.city?.contains(searchQuery, ignoreCase = true) == true)

            val matchesCategory = when (selectedAmenity) {
                "Favorites" -> favoriteIds.contains(r.id)
                "Wishlist" -> wishlistMap.containsKey(r.id)
                "All", null -> true
                else -> r.amenity.equals(selectedAmenity, ignoreCase = true)
            }

            matchesQuery && matchesCategory
        }.map { r ->
            val dist = DistanceCalculator.calculateDistanceMiles(
                selectedAnchor.latitude, selectedAnchor.longitude,
                r.latitude, r.longitude
            )
            Pair(r, dist)
        }.sortedBy { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Near " + selectedAnchor.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { anchorMenuExpanded = true }) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Change Anchor")
                        }
                        DropdownMenu(
                            expanded = anchorMenuExpanded,
                            onDismissRequest = { anchorMenuExpanded = false }
                        ) {
                            LocationManager.PRESET_ANCHORS.forEach { anchor ->
                                DropdownMenuItem(
                                    text = { Text(anchor.name) },
                                    onClick = {
                                        onAnchorSelected(anchor)
                                        anchorMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onBackupClick) {
                        Icon(Icons.Default.Backup, contentDescription = "Export Backup")
                    }
                    IconButton(onClick = onRestoreClick) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore Backup")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name, cuisine, city...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                items(categories) { category ->
                    val isSelected = (selectedAmenity == category) || (selectedAmenity == null && category == "All")
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAmenity = if (category == "All") null else category },
                        label = { Text(category.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredRestaurants.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No restaurants found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredRestaurants) { (restaurant, distMiles) ->
                        RestaurantCard(
                            restaurant = restaurant,
                            distanceFormatted = DistanceCalculator.formatDistance(distMiles),
                            isFavorite = favoriteIds.contains(restaurant.id),
                            wishlist = wishlistMap[restaurant.id],
                            dishes = dishWishlistMap[restaurant.id] ?: emptyList(),
                            favoriteDishes = favoriteDishMap[restaurant.id] ?: emptyList(),
                            onFavoriteToggle = { onFavoriteToggle(restaurant.id) },
                            onWishlistClick = { onWishlistClick(restaurant) },
                            onClick = { onRestaurantClick(restaurant) },
                            onNavigateClick = { onNavigateClick(restaurant) },
                            onCallClick = { onCallClick(restaurant) },
                            onWebsiteClick = { onWebsiteClick(restaurant) },
                            onShareClick = { onShareClick(restaurant) }
                        )
                    }
                }
            }
        }
    }
}

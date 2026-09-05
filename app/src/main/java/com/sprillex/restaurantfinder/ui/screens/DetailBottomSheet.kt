package com.sprillex.restaurantfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import com.sprillex.restaurantfinder.data.DishWishlist
import com.sprillex.restaurantfinder.data.Wishlist
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailBottomSheet(
    restaurant: Restaurant,
    isFavorite: Boolean,
    wishlist: Wishlist?,
    dishes: List<DishWishlist>,
    onFavoriteToggle: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddDishClick: () -> Unit,
    onDeleteDishClick: (DishWishlist) -> Unit,
    onDismiss: () -> Unit,
    onNavigateClick: () -> Unit,
    onCallClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceLevel2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else TextSecondary
                        )
                    }
                    IconButton(onClick = onWishlistClick) {
                        Icon(
                            imageVector = if (wishlist != null) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Wishlist",
                            tint = if (wishlist != null) MaterialTheme.colorScheme.tertiary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val category = restaurant.cuisine ?: restaurant.amenity.replace('_', ' ')
            Text(
                text = "Category: ${category.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            val streetLine = listOfNotNull(restaurant.housenumber, restaurant.street)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val fullAddress = listOfNotNull(streetLine.ifBlank { null }, restaurant.city, restaurant.postcode)
                .filter { it.isNotBlank() }
                .joinToString(", ")

            if (fullAddress.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Address: $fullAddress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dishes I Want to Try (${dishes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onAddDishClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Dish",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (dishes.isEmpty()) {
                Text(
                    text = "No dishes added yet. Tap '+' to add a dish to try!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    dishes.forEach { dish ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dish.dishName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                    if (dish.notes.isNotBlank()) {
                                        Text(
                                            text = dish.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteDishClick(dish) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Dish",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (wishlist != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Wishlist Priority: ${wishlist.priority}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        if (wishlist.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Notes: ${wishlist.notes}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            if (!restaurant.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phone: ${restaurant.phone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onNavigateClick) {
                    Icon(Icons.Default.Navigation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Navigate")
                }

                if (!restaurant.phone.isNullOrBlank()) {
                    OutlinedButton(onClick = onCallClick) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call")
                    }
                }

                if (!restaurant.website.isNullOrBlank()) {
                    OutlinedButton(onClick = onWebsiteClick) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Website")
                    }
                }

                OutlinedButton(onClick = onShareClick) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}

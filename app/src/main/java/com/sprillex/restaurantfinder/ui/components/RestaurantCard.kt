package com.sprillex.restaurantfinder.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.DishWishlist
import com.sprillex.restaurantfinder.data.FavoriteDish
import com.sprillex.restaurantfinder.data.PhotoStorageManager
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantPhoto
import com.sprillex.restaurantfinder.data.Wishlist
import com.sprillex.restaurantfinder.ui.theme.DarkBgBody
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel1
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.ErrorPastelRed
import com.sprillex.restaurantfinder.ui.theme.OpenPastelGreen
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary
import com.sprillex.restaurantfinder.utils.OpeningHoursParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestaurantCard(
    restaurant: Restaurant,
    distanceFormatted: String,
    isFavorite: Boolean,
    wishlist: Wishlist?,
    dishes: List<DishWishlist>,
    favoriteDishes: List<FavoriteDish>,
    isClosedOverride: Boolean = false,
    photos: List<RestaurantPhoto> = emptyList(),
    onFavoriteToggle: () -> Unit,
    onWishlistClick: () -> Unit,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit,
    onCallClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val context = LocalContext.current
    val firstPhoto = photos.firstOrNull()
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = firstPhoto?.relativePath) {
        value = if (firstPhoto != null) {
            withContext(Dispatchers.IO) {
                val file = PhotoStorageManager.resolvePhotoFile(context, firstPhoto.relativePath)
                PhotoStorageManager.loadScaledBitmap(file, maxEdgePx = 240)
            }
        } else {
            null
        }
    }

    val openStatus: Boolean? = if (isClosedOverride) {
        false
    } else {
        OpeningHoursParser.isOpenNow(restaurant.opening_hours)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (photos.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceLevel2),
                        contentAlignment = Alignment.Center
                    ) {
                        val bmp = thumbnailBitmap
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = firstPhoto?.caption?.ifBlank { "Venue photo" } ?: "Venue photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Photo,
                                contentDescription = "Photo attached",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (photos.size > 1) {
                            Surface(
                                color = DarkBgBody.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(2.dp)
                            ) {
                                Text(
                                    text = "${photos.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text(
                        text = distanceFormatted,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val category = restaurant.cuisine ?: restaurant.amenity.replace('_', ' ')
            val streetLine = listOfNotNull(restaurant.housenumber, restaurant.street)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val address = listOfNotNull(streetLine.ifBlank { null }, restaurant.city)
                .filter { it.isNotBlank() }
                .joinToString(", ")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (openStatus != null) {
                    Surface(
                        color = DarkSurfaceLevel2,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (openStatus) "Open" else "Closed",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (openStatus) OpenPastelGreen else ErrorPastelRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (address.isNotBlank()) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (wishlist != null || dishes.isNotEmpty() || favoriteDishes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (wishlist != null) {
                        AssistChip(
                            onClick = onWishlistClick,
                            label = {
                                Text(
                                    text = "Wishlist [${wishlist.priority}]" + if (wishlist.notes.isNotBlank()) ": ${wishlist.notes}" else "",
                                    maxLines = 1
                                )
                            }
                        )
                    }
                    if (dishes.isNotEmpty()) {
                        AssistChip(
                            onClick = onClick,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.RestaurantMenu,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(text = "${dishes.size} to try")
                            }
                        )
                    }
                    if (favoriteDishes.isNotEmpty()) {
                        AssistChip(
                            onClick = onClick,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(text = "${favoriteDishes.size} fav dish${if (favoriteDishes.size > 1) "es" else ""}")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateClick) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Navigate",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (!restaurant.phone.isNullOrBlank()) {
                    IconButton(onClick = onCallClick) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (!restaurant.website.isNullOrBlank()) {
                    IconButton(onClick = onWebsiteClick) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Website",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

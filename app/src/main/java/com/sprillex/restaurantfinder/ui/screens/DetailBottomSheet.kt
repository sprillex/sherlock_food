package com.sprillex.restaurantfinder.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sprillex.restaurantfinder.data.CustomRestaurant
import com.sprillex.restaurantfinder.data.DishWishlist
import com.sprillex.restaurantfinder.data.FavoriteDish
import com.sprillex.restaurantfinder.data.ManualInsight
import com.sprillex.restaurantfinder.data.PhotoStorageManager
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantDetailEntity
import com.sprillex.restaurantfinder.data.RestaurantPhoto
import com.sprillex.restaurantfinder.data.VisitLog
import com.sprillex.restaurantfinder.data.Wishlist
import com.sprillex.restaurantfinder.ui.theme.BrandPastelBlue
import com.sprillex.restaurantfinder.ui.theme.DarkBgBody
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel1
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.ErrorPastelRed
import com.sprillex.restaurantfinder.ui.theme.OpenPastelGreen
import com.sprillex.restaurantfinder.ui.theme.StarPastelAmber
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary
import com.sprillex.restaurantfinder.utils.OpeningHoursParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun DetailBottomSheet(
    restaurant: Restaurant,
    details: RestaurantDetailEntity? = null,
    isLoadingEnrichment: Boolean = false,
    onEnrich: () -> Unit = {},
    onRefreshEnrich: () -> Unit = {},
    hasOverride: Boolean = false,
    isClosedOverride: Boolean = false,
    manualInsight: ManualInsight? = null,
    photos: List<RestaurantPhoto> = emptyList(),
    visitLogs: List<VisitLog> = emptyList(),
    onEditRestaurantClick: () -> Unit = {},
    onEditManualInsightClick: () -> Unit = {},
    onAddPhotoCameraClick: () -> Unit = {},
    onAddPhotoGalleryClick: () -> Unit = {},
    onEditPhotoClick: (RestaurantPhoto) -> Unit = {},
    onSharePhotoClick: (RestaurantPhoto) -> Unit = {},
    onDeletePhotoClick: (RestaurantPhoto) -> Unit = {},
    onAddVisitLogClick: () -> Unit = {},
    onEditVisitLogClick: (VisitLog) -> Unit = {},
    onShareVisitLogClick: (VisitLog) -> Unit = {},
    onDeleteVisitLogClick: (VisitLog) -> Unit = {},
    isFavorite: Boolean,
    wishlist: Wishlist?,
    dishes: List<DishWishlist>,
    favoriteDishes: List<FavoriteDish>,
    onFavoriteToggle: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddDishClick: () -> Unit,
    onEditDishClick: (DishWishlist) -> Unit = {},
    onShareDishClick: (DishWishlist) -> Unit = {},
    onDeleteDishClick: (DishWishlist) -> Unit,
    onAddFavoriteDishClick: () -> Unit,
    onEditFavoriteDishClick: (FavoriteDish) -> Unit = {},
    onShareFavoriteDishClick: (FavoriteDish) -> Unit = {},
    onDeleteFavoriteDishClick: (FavoriteDish) -> Unit,
    onDismiss: () -> Unit,
    onNavigateClick: () -> Unit,
    onCallClick: () -> Unit,
    onWebsiteClick: () -> Unit,
    onShareClick: () -> Unit
) {
    LaunchedEffect(restaurant.id) {
        onEnrich()
    }

    val haptic = LocalHapticFeedback.current
    var addPhotoMenuExpanded by remember { mutableStateOf(false) }
    var previewPhotoId by remember { mutableStateOf<Long?>(null) }
    var photoPendingDelete by remember { mutableStateOf<RestaurantPhoto?>(null) }

    val isCustomVenue = CustomRestaurant.isCustomId(restaurant.id)
    val openStatus: Boolean? = if (isClosedOverride) {
        false
    } else {
        OpeningHoursParser.isOpenNow(restaurant.opening_hours)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceLevel2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
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
                    IconButton(onClick = onEditRestaurantClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Venue Fields",
                            tint = BrandPastelBlue
                        )
                    }
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
                            contentDescription = "Want to Try",
                            tint = if (wishlist != null) MaterialTheme.colorScheme.tertiary else TextSecondary
                        )
                    }
                }
            }

            if (isCustomVenue || hasOverride || openStatus != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCustomVenue) {
                        StatusBadgePill(label = "Custom Venue", textColor = BrandPastelBlue)
                    }
                    if (isClosedOverride) {
                        StatusBadgePill(label = "Marked Closed", textColor = ErrorPastelRed)
                    } else if (openStatus != null) {
                        StatusBadgePill(
                            label = if (openStatus) "Open Now" else "Closed",
                            textColor = if (openStatus) OpenPastelGreen else ErrorPastelRed
                        )
                    }
                    if (hasOverride && !isCustomVenue) {
                        StatusBadgePill(label = "Edited Locally", textColor = BrandPastelBlue)
                    }
                }
            }

            if (!restaurant.brand.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Brand: ${restaurant.brand}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
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

            if (!restaurant.opening_hours.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Opening Hours",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hours: ${restaurant.opening_hours}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Services, Accessibility, Dietary Chips
            val serviceBadges = mutableListOf<String>()
            restaurant.delivery?.let {
                when (it) {
                    "yes" -> serviceBadges.add("Delivery")
                    "only" -> serviceBadges.add("Delivery Only")
                    else -> {}
                }
            }
            restaurant.takeaway?.let {
                when (it) {
                    "yes" -> serviceBadges.add("Takeaway")
                    "only" -> serviceBadges.add("Takeaway Only")
                    else -> {}
                }
            }
            restaurant.drive_through?.let {
                if (it == "yes") serviceBadges.add("Drive-Through")
            }
            restaurant.outdoor_seating?.let {
                if (it == "yes") serviceBadges.add("Outdoor Seating")
            }

            val dietaryBadges = mutableListOf<String>()
            restaurant.diet_vegetarian?.let {
                when (it) {
                    "yes" -> dietaryBadges.add("Vegetarian")
                    "only" -> dietaryBadges.add("Vegetarian Only")
                    else -> {}
                }
            }
            restaurant.diet_vegan?.let {
                when (it) {
                    "yes" -> dietaryBadges.add("Vegan")
                    "only" -> dietaryBadges.add("Vegan Only")
                    else -> {}
                }
            }
            restaurant.diet_gluten_free?.let {
                when (it) {
                    "yes" -> dietaryBadges.add("Gluten-Free")
                    "only" -> dietaryBadges.add("Gluten-Free Only")
                    else -> {}
                }
            }

            val accessibilityBadge = restaurant.wheelchair?.let {
                when (it) {
                    "yes" -> "Wheelchair Accessible"
                    "limited" -> "Limited Wheelchair Access"
                    "designated" -> "Designated Wheelchair Access"
                    else -> null
                }
            }

            val allBadges = serviceBadges + dietaryBadges + listOfNotNull(accessibilityBadge)

            if (allBadges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    allBadges.forEach { badge ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(badge, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Photos Gallery Strip
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        tint = BrandPastelBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Photos (${photos.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }

                Box {
                    OutlinedButton(onClick = { addPhotoMenuExpanded = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Photo")
                    }
                    DropdownMenu(
                        expanded = addPhotoMenuExpanded,
                        onDismissRequest = { addPhotoMenuExpanded = false },
                        modifier = Modifier.background(DarkSurfaceLevel2)
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                            text = { Text("Take Photo") },
                            onClick = {
                                addPhotoMenuExpanded = false
                                onAddPhotoCameraClick()
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                            text = { Text("Choose from Gallery") },
                            onClick = {
                                addPhotoMenuExpanded = false
                                onAddPhotoGalleryClick()
                            }
                        )
                    }
                }
            }

            if (photos.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No photos yet. Add food, menu, exterior, or interior photos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(photos, key = { it.id }) { photo ->
                        var photoMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            PhotoStripItem(
                                photo = photo,
                                onClick = { previewPhotoId = photo.id },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    photoMenuExpanded = true
                                }
                            )
                            ItemContextMenu(
                                expanded = photoMenuExpanded,
                                onDismiss = { photoMenuExpanded = false },
                                onEdit = { onEditPhotoClick(photo) },
                                onShare = { onSharePhotoClick(photo) },
                                onDelete = { photoPendingDelete = photo }
                            )
                        }
                    }
                }
            }

            // Pinned Manual Dining Insights Card (above AI Dining Insights)
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "Manual Dining Insights",
                                tint = BrandPastelBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Personal Dining Notes",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            StatusBadgePill(label = "Manual", textColor = BrandPastelBlue)
                        }
                        IconButton(onClick = onEditManualInsightClick) {
                            Icon(
                                imageVector = if (manualInsight == null) Icons.Default.Add else Icons.Default.Edit,
                                contentDescription = "Edit Manual Insights",
                                tint = BrandPastelBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (manualInsight == null) {
                        Text(
                            text = "Pin your own summary, go-to dishes, price tier, and parking tips above AI notes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        if (manualInsight.summary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = manualInsight.summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        val manualChips = buildList {
                            if (manualInsight.priceTier.isNotBlank()) add("Price: ${manualInsight.priceTier}")
                            if (manualInsight.parkingNotes.isNotBlank()) add("Parking: ${manualInsight.parkingNotes}")
                        }
                        if (manualChips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                manualChips.forEach { chip ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text(chip, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                        if (manualInsight.recommendedDishes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Recommended: ${manualInsight.recommendedDishes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandPastelBlue
                            )
                        }
                        if (manualInsight.goodToKnow.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Good to know: ${manualInsight.goodToKnow}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // AI Dining Insights Card
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Enrichment",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "AI Dining Insights",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            StatusBadgePill(label = "AI", textColor = TextSecondary)
                        }
                        if (isLoadingEnrichment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = onRefreshEnrich) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh AI Insights",
                                    tint = BrandPastelBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isLoadingEnrichment && details == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Checking local dining notes...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else if (details == null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No cached AI insights yet. Tap refresh to query Gemini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    } else {
                        if (!details.editorialSummary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = details.editorialSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }

                        val infoItems = mutableListOf<Pair<String, String>>()
                        details.priceTier?.let { if (it != "unknown") infoItems.add("Price" to it) }
                        details.serviceModel?.let { if (it != "unknown") infoItems.add("Service" to it.replace('_', ' ').replaceFirstChar { c -> c.uppercase() }) }
                        details.parking?.let { if (it != "unknown") infoItems.add("Parking" to it.replace('_', ' ').replaceFirstChar { c -> c.uppercase() }) }
                        details.outdoorPatio?.let { if (it != "unknown") infoItems.add("Patio" to it.replaceFirstChar { c -> c.uppercase() }) }

                        if (infoItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                infoItems.forEach { (label, value) ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("$label: $value", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        if (!details.signatureItems.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Signature Items:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                details.signatureItems.forEach { item ->
                                    AssistChip(
                                        onClick = { },
                                        label = { Text(item, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        if (!details.vibeTags.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                details.vibeTags.forEach { tag ->
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        if (!details.goodToKnow.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Good to know: ${details.goodToKnow}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Visit Log Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = BrandPastelBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Visit Log (${visitLogs.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (visitLogs.isNotEmpty()) {
                        val avg = visitLogs.map { it.rating }.average()
                        StatusBadgePill(
                            label = "★ ${String.format(Locale.US, "%.1f", avg)}",
                            textColor = StarPastelAmber
                        )
                    }
                }
                OutlinedButton(onClick = onAddVisitLogClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Visit")
                }
            }

            if (visitLogs.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No visits logged yet. Tap 'Log Visit' to record a rating and what you ordered.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                    visitLogs.sortedByDescending { it.visitedAt }.forEach { visit ->
                        var visitMenuExpanded by remember(visit.id) { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceLevel1),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            visitMenuExpanded = true
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = dateFormat.format(Date(visit.visitedAt)),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            for (s in 1..5) {
                                                Icon(
                                                    imageVector = if (s <= visit.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                                    contentDescription = null,
                                                    tint = if (s <= visit.rating) StarPastelAmber else TextSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (visit.orderedDishes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Ordered: ${visit.orderedDishes}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                    if (visit.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = visit.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            ItemContextMenu(
                                expanded = visitMenuExpanded,
                                onDismiss = { visitMenuExpanded = false },
                                onEdit = { onEditVisitLogClick(visit) },
                                onShare = { onShareVisitLogClick(visit) },
                                onDelete = { onDeleteVisitLogClick(visit) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Favorite Dishes Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Favorite Menu Items (${favoriteDishes.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                IconButton(onClick = onAddFavoriteDishClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Favorite Dish",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (favoriteDishes.isEmpty()) {
                Text(
                    text = "No favorite menu items added yet. Tap '+' to add your go-to dishes!",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    favoriteDishes.forEach { favDish ->
                        var favDishMenuExpanded by remember(favDish.id) { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            favDishMenuExpanded = true
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = favDish.dishName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                    if (favDish.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = favDish.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            ItemContextMenu(
                                expanded = favDishMenuExpanded,
                                onDismiss = { favDishMenuExpanded = false },
                                onEdit = { onEditFavoriteDishClick(favDish) },
                                onShare = { onShareFavoriteDishClick(favDish) },
                                onDelete = { onDeleteFavoriteDishClick(favDish) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Dishes to try Section
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
                        var dishMenuExpanded by remember(dish.id) { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            dishMenuExpanded = true
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = dish.dishName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                    if (dish.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dish.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            ItemContextMenu(
                                expanded = dishMenuExpanded,
                                onDismiss = { dishMenuExpanded = false },
                                onEdit = { onEditDishClick(dish) },
                                onShare = { onShareDishClick(dish) },
                                onDelete = { onDeleteDishClick(dish) }
                            )
                        }
                    }
                }
            }

            if (wishlist != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWishlistClick() }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Want to Try · Priority: ${wishlist.priority}",
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Phone: ${restaurant.phone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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

    val activePreviewId = previewPhotoId
    if (activePreviewId != null && photos.isNotEmpty()) {
        val initialIndex = photos.indexOfFirst { it.id == activePreviewId }.coerceAtLeast(0)
        PhotoPagerDialog(
            photos = photos,
            initialIndex = initialIndex,
            onSharePhoto = onSharePhotoClick,
            onEditPhoto = onEditPhotoClick,
            onRequestDelete = { photo -> photoPendingDelete = photo },
            onDismiss = { previewPhotoId = null }
        )
    }

    photoPendingDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoPendingDelete = null },
            title = { Text("Delete Photo?", color = TextPrimary) },
            text = {
                Text(
                    text = "Remove this ${photo.category} photo from your local library?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePhotoClick(photo)
                        photoPendingDelete = null
                        if (previewPhotoId == photo.id && photos.size <= 1) {
                            previewPhotoId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorPastelRed)
                ) {
                    Text("Delete", color = DarkBgBody)
                }
            },
            dismissButton = {
                TextButton(onClick = { photoPendingDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceLevel2
        )
    }
}

@Composable
private fun ItemContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(DarkSurfaceLevel2)
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = BrandPastelBlue
                )
            },
            text = { Text("Edit", color = TextPrimary) },
            onClick = {
                onDismiss()
                onEdit()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = BrandPastelBlue
                )
            },
            text = { Text("Share", color = TextPrimary) },
            onClick = {
                onDismiss()
                onShare()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = ErrorPastelRed
                )
            },
            text = { Text("Delete", color = ErrorPastelRed) },
            onClick = {
                onDismiss()
                onDelete()
            }
        )
    }
}

@Composable
private fun StatusBadgePill(
    label: String,
    textColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = DarkSurfaceLevel1,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoStripItem(
    photo: RestaurantPhoto,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photo.relativePath) {
        value = withContext(Dispatchers.IO) {
            val file = PhotoStorageManager.resolvePhotoFile(context, photo.relativePath)
            PhotoStorageManager.loadScaledBitmap(file, maxEdgePx = 400)
        }
    }

    Column(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 92.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceLevel1),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = photo.caption.ifBlank { photo.category },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = "Missing",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Surface(
                color = DarkBgBody.copy(alpha = 0.82f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            ) {
                Text(
                    text = photo.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandPastelBlue,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (photo.caption.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = photo.caption,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoPagerDialog(
    photos: List<RestaurantPhoto>,
    initialIndex: Int,
    onSharePhoto: (RestaurantPhoto) -> Unit,
    onEditPhoto: (RestaurantPhoto) -> Unit,
    onRequestDelete: (RestaurantPhoto) -> Unit,
    onDismiss: () -> Unit
) {
    val safeInitialIndex = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(
        initialPage = safeInitialIndex,
        pageCount = { photos.size }
    )
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        isCurrentPageZoomed = false
    }

    val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.firstOrNull() ?: return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = DarkBgBody.copy(alpha = 0.96f),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadgePill(label = currentPhoto.category, textColor = BrandPastelBlue)
                        StatusBadgePill(
                            label = "${pagerState.currentPage + 1} / ${photos.size}",
                            textColor = TextPrimary
                        )
                    }
                    Row {
                        IconButton(onClick = { onSharePhoto(currentPhoto) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share Photo",
                                tint = BrandPastelBlue
                            )
                        }
                        IconButton(onClick = { onEditPhoto(currentPhoto) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Photo Details",
                                tint = BrandPastelBlue
                            )
                        }
                        IconButton(onClick = { onRequestDelete(currentPhoto) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Photo",
                                tint = ErrorPastelRed
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Preview",
                                tint = TextPrimary
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isCurrentPageZoomed,
                    key = { index -> photos.getOrNull(index)?.id ?: index },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val pagePhoto = photos[page]
                    ZoomablePhotoPage(
                        photo = pagePhoto,
                        isActivePage = page == pagerState.currentPage,
                        onZoomStateChanged = { zoomed ->
                            if (page == pagerState.currentPage) {
                                isCurrentPageZoomed = zoomed
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = DarkSurfaceLevel2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditPhoto(currentPhoto) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPhoto.caption.ifBlank { "Tap to add caption or edit category..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentPhoto.caption.isNotBlank()) TextPrimary else TextSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Caption",
                            tint = BrandPastelBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomablePhotoPage(
    photo: RestaurantPhoto,
    isActivePage: Boolean,
    onZoomStateChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photo.relativePath) {
        value = withContext(Dispatchers.IO) {
            val file = PhotoStorageManager.resolvePhotoFile(context, photo.relativePath)
            PhotoStorageManager.loadScaledBitmap(file, maxEdgePx = PhotoStorageManager.MAX_EDGE_PX)
        }
    }

    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(isActivePage) {
        if (!isActivePage && scale != 1f) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    LaunchedEffect(scale) {
        onZoomStateChanged(scale > 1.01f)
    }

    fun clampOffset(candidate: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.01f || containerSize == IntSize.Zero) return Offset.Zero
        val maxX = (containerSize.width * (targetScale - 1f)) / 2f
        val maxY = (containerSize.height * (targetScale - 1f)) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(photo.id) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2.5f
                            val centerX = containerSize.width / 2f
                            val centerY = containerSize.height / 2f
                            val desiredOffset = Offset(
                                x = (centerX - tapOffset.x) * (targetScale - 1f),
                                y = (centerY - tapOffset.y) * (targetScale - 1f)
                            )
                            scale = targetScale
                            offset = clampOffset(desiredOffset, targetScale)
                        }
                    }
                )
            }
            .pointerInput(photo.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val isMultiTouch = pressedCount >= 2
                        val isZoomed = scale > 1.01f

                        if (isMultiTouch || isZoomed) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
                            val nextOffset = if (nextScale <= 1.01f) {
                                Offset.Zero
                            } else {
                                clampOffset(offset + panChange, nextScale)
                            }
                            scale = nextScale
                            offset = nextOffset
                            if (isMultiTouch || nextScale > 1.01f) {
                                event.changes.forEach { change ->
                                    if (change.pressed) {
                                        change.consume()
                                    }
                                }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = photo.caption.ifBlank { photo.category },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else {
            Text(
                text = "Photo file unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

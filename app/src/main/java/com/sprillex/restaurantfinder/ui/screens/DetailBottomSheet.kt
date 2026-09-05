package com.sprillex.restaurantfinder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.DishWishlist
import com.sprillex.restaurantfinder.data.FavoriteDish
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantDetailEntity
import com.sprillex.restaurantfinder.data.Wishlist
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel1
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailBottomSheet(
    restaurant: Restaurant,
    details: RestaurantDetailEntity? = null,
    isLoadingEnrichment: Boolean = false,
    onEnrich: () -> Unit = {},
    isFavorite: Boolean,
    wishlist: Wishlist?,
    dishes: List<DishWishlist>,
    favoriteDishes: List<FavoriteDish>,
    onFavoriteToggle: () -> Unit,
    onWishlistClick: () -> Unit,
    onAddDishClick: () -> Unit,
    onDeleteDishClick: (DishWishlist) -> Unit,
    onAddFavoriteDishClick: () -> Unit,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceLevel2
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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

            if (!restaurant.brand.isNullOrBlank()) {
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

            // AI Dining Insights Card
            if (details != null || isLoadingEnrichment) {
                Spacer(modifier = Modifier.height(14.dp))
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Enrichment",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Dining Insights",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                            }
                            if (isLoadingEnrichment && details == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isLoadingEnrichment && details == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Checking local dining notes...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        } else if (details != null) {
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
                                        text = favDish.dishName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary
                                    )
                                    if (favDish.notes.isNotBlank()) {
                                        Text(
                                            text = favDish.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteFavoriteDishClick(favDish) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Favorite Dish",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
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

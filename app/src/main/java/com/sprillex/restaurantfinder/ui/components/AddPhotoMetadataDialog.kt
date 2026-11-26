package com.sprillex.restaurantfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantPhoto
import com.sprillex.restaurantfinder.ui.theme.BrandPastelBlue
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPhotoMetadataDialog(
    restaurant: Restaurant,
    existingPhoto: RestaurantPhoto? = null,
    onSave: (category: String, caption: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember(existingPhoto?.id) {
        mutableStateOf(
            existingPhoto?.category?.takeIf { it in RestaurantPhoto.CATEGORIES }
                ?: RestaurantPhoto.CATEGORIES.first()
        )
    }
    var caption by remember(existingPhoto?.id) {
        mutableStateOf(existingPhoto?.caption.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingPhoto != null) "Edit Photo Details" else "Photo Details",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandPastelBlue
                )

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RestaurantPhoto.CATEGORIES.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) }
                        )
                    }
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption (optional)") },
                    placeholder = { Text("e.g. Patio seating, daily specials board") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedCategory, caption.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPastelBlue)
            ) {
                Text(if (existingPhoto != null) "Save" else "Save Photo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurfaceLevel2
    )
}

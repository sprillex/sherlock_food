package com.sprillex.restaurantfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.DishWishlist
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.TextPrimary

@Composable
fun AddDishDialog(
    restaurant: Restaurant,
    existingDish: DishWishlist? = null,
    onSave: (dishName: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var dishName by remember(existingDish?.id) { mutableStateOf(existingDish?.dishName.orEmpty()) }
    var notes by remember(existingDish?.id) { mutableStateOf(existingDish?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingDish != null) "Edit Dish to Try" else "Add Dish to Try",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = dishName,
                    onValueChange = { dishName = it },
                    label = { Text("Dish Name (e.g. Garlic Ramen)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Extra spicy, add egg)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dishName.isNotBlank()) {
                        onSave(dishName.trim(), notes.trim())
                    }
                },
                enabled = dishName.isNotBlank()
            ) {
                Text(if (existingDish != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = DarkSurfaceLevel2
    )
}

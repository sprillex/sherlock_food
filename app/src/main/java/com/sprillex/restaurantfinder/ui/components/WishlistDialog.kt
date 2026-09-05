package com.sprillex.restaurantfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.Wishlist
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistDialog(
    restaurant: Restaurant,
    currentWishlist: Wishlist?,
    onSave: (notes: String, priority: String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember { mutableStateOf(currentWishlist?.notes ?: "") }
    var selectedPriority by remember { mutableStateOf(currentWishlist?.priority ?: "Medium") }

    val priorities = listOf("Low", "Medium", "High")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentWishlist != null) "Edit Wishlist Item" else "Add to Wishlist",
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Priority:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorities.forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Want to try signature burger)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(notes, selectedPriority)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (currentWishlist != null) {
                    TextButton(onClick = onDelete) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        containerColor = DarkSurfaceLevel2
    )
}

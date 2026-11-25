package com.sprillex.restaurantfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.ManualInsight
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.ui.theme.BrandPastelBlue
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.ErrorPastelRed
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInsightDialog(
    restaurant: Restaurant,
    currentInsight: ManualInsight?,
    onSave: (ManualInsight) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var summary by remember { mutableStateOf(currentInsight?.summary.orEmpty()) }
    var recommendedDishes by remember { mutableStateOf(currentInsight?.recommendedDishes.orEmpty()) }
    var priceTier by remember { mutableStateOf(currentInsight?.priceTier.orEmpty()) }
    var parkingNotes by remember { mutableStateOf(currentInsight?.parkingNotes.orEmpty()) }
    var goodToKnow by remember { mutableStateOf(currentInsight?.goodToKnow.orEmpty()) }

    val priceOptions = listOf("$", "$$", "$$$", "$$$$")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (currentInsight != null) "Edit Manual Insights" else "Add Manual Insights",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandPastelBlue
                )

                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text("Personal Summary") },
                    placeholder = { Text("Vibe, quality, why you come here...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = recommendedDishes,
                    onValueChange = { recommendedDishes = it },
                    label = { Text("Recommended Dishes") },
                    placeholder = { Text("e.g. Birria tacos, horchata") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Price Tier", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    priceOptions.forEach { tier ->
                        FilterChip(
                            selected = priceTier == tier,
                            onClick = { priceTier = if (priceTier == tier) "" else tier },
                            label = { Text(tier) }
                        )
                    }
                }

                OutlinedTextField(
                    value = parkingNotes,
                    onValueChange = { parkingNotes = it },
                    label = { Text("Parking Notes") },
                    placeholder = { Text("e.g. Free lot behind building") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = goodToKnow,
                    onValueChange = { goodToKnow = it },
                    label = { Text("Good to Know") },
                    placeholder = { Text("e.g. Cash only, patio opens at 5pm") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val hasAnyContent = summary.isNotBlank() ||
                recommendedDishes.isNotBlank() ||
                priceTier.isNotBlank() ||
                parkingNotes.isNotBlank() ||
                goodToKnow.isNotBlank()

            Button(
                onClick = {
                    onSave(
                        ManualInsight(
                            restaurantId = restaurant.id,
                            summary = summary.trim(),
                            recommendedDishes = recommendedDishes.trim(),
                            priceTier = priceTier.trim(),
                            parkingNotes = parkingNotes.trim(),
                            goodToKnow = goodToKnow.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                },
                enabled = hasAnyContent,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPastelBlue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (currentInsight != null && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Clear", color = ErrorPastelRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        },
        containerColor = DarkSurfaceLevel2
    )
}

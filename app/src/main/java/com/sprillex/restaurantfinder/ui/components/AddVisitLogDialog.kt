package com.sprillex.restaurantfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.VisitLog
import com.sprillex.restaurantfinder.ui.theme.BrandPastelBlue
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.StarPastelAmber
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitLogDialog(
    restaurant: Restaurant,
    existingVisitLog: VisitLog? = null,
    onSave: (visitedAt: Long, rating: Int, orderedDishes: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var visitedAt by remember(existingVisitLog?.id) {
        mutableLongStateOf(existingVisitLog?.visitedAt ?: System.currentTimeMillis())
    }
    var rating by remember(existingVisitLog?.id) {
        mutableIntStateOf((existingVisitLog?.rating ?: 5).coerceIn(1, 5))
    }
    var orderedDishes by remember(existingVisitLog?.id) {
        mutableStateOf(existingVisitLog?.orderedDishes.orEmpty())
    }
    var notes by remember(existingVisitLog?.id) {
        mutableStateOf(existingVisitLog?.notes.orEmpty())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingVisitLog != null) "Edit Visit Log" else "Log Visit",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = restaurant.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandPastelBlue
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date: ${dateFormat.format(Date(visitedAt))}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Change Visit Date",
                            tint = BrandPastelBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Change", color = BrandPastelBlue)
                    }
                }

                Column {
                    Text(
                        text = "Your Rating ($rating / 5)",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (star in 1..5) {
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) StarPastelAmber else TextSecondary
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = orderedDishes,
                    onValueChange = { orderedDishes = it },
                    label = { Text("Ordered Dishes") },
                    placeholder = { Text("e.g. Reuben sandwich, kettle chips") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Visit Notes") },
                    placeholder = { Text("Service, wait time, highlights...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(visitedAt, rating.coerceIn(1, 5), orderedDishes.trim(), notes.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPastelBlue)
            ) {
                Text(if (existingVisitLog != null) "Save" else "Save Visit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurfaceLevel2
    )

    if (showDatePicker) {
        val localCal = remember(visitedAt) {
            Calendar.getInstance().apply { timeInMillis = visitedAt }
        }
        val initialUtcMillis = remember(visitedAt) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(
                    localCal.get(Calendar.YEAR),
                    localCal.get(Calendar.MONTH),
                    localCal.get(Calendar.DAY_OF_MONTH)
                )
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedUtc ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = selectedUtc
                            }
                            val updatedLocal = Calendar.getInstance().apply {
                                timeInMillis = visitedAt
                                set(
                                    utcCal.get(Calendar.YEAR),
                                    utcCal.get(Calendar.MONTH),
                                    utcCal.get(Calendar.DAY_OF_MONTH)
                                )
                            }
                            visitedAt = updatedLocal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = BrandPastelBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

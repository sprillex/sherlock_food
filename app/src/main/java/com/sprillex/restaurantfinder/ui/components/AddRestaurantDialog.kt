package com.sprillex.restaurantfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.CustomRestaurant
import com.sprillex.restaurantfinder.location.AnchorLocation
import com.sprillex.restaurantfinder.ui.theme.BrandPastelBlue
import com.sprillex.restaurantfinder.ui.theme.DarkBgBody
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.ErrorPastelRed
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRestaurantDialog(
    currentAnchor: AnchorLocation,
    existingCustomRestaurant: CustomRestaurant? = null,
    isAlreadyInWantToTry: Boolean = false,
    onSave: (
        customRestaurant: CustomRestaurant,
        addToWantToTry: Boolean,
        wantToTryPriority: String,
        wantToTryNotes: String
    ) -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isEditing = existingCustomRestaurant != null
    var name by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.name.orEmpty())
    }
    var amenity by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.amenity ?: "restaurant")
    }
    var cuisine by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.cuisine.orEmpty())
    }
    var housenumber by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.housenumber.orEmpty())
    }
    var street by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.street.orEmpty())
    }
    var city by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.city.orEmpty())
    }
    var postcode by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.postcode.orEmpty())
    }
    var phone by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.phone.orEmpty())
    }
    var website by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.website.orEmpty())
    }
    var openingHours by remember(existingCustomRestaurant?.id) {
        mutableStateOf(existingCustomRestaurant?.openingHours.orEmpty())
    }

    var takeaway by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.takeaway) }
    var delivery by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.delivery) }
    var outdoorSeating by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.outdoorSeating) }
    var driveThrough by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.driveThrough) }
    var wheelchair by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.wheelchair) }
    var dietVegetarian by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.dietVegetarian) }
    var dietVegan by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.dietVegan) }
    var dietGlutenFree by remember(existingCustomRestaurant?.id) { mutableStateOf(existingCustomRestaurant?.dietGlutenFree) }

    var latitudeText by remember(existingCustomRestaurant?.id) {
        mutableStateOf(
            String.format(
                Locale.US,
                "%.6f",
                existingCustomRestaurant?.latitude ?: currentAnchor.latitude
            )
        )
    }
    var longitudeText by remember(existingCustomRestaurant?.id) {
        mutableStateOf(
            String.format(
                Locale.US,
                "%.6f",
                existingCustomRestaurant?.longitude ?: currentAnchor.longitude
            )
        )
    }

    var addToWantToTry by remember(existingCustomRestaurant?.id) {
        mutableStateOf(false)
    }
    var wantToTryPriority by remember { mutableStateOf("Medium") }
    var wantToTryNotes by remember { mutableStateOf("") }
    var confirmDeleteOpen by remember { mutableStateOf(false) }

    val parsedLat = latitudeText.trim().toDoubleOrNull()
    val parsedLon = longitudeText.trim().toDoubleOrNull()
    val coordsValid = parsedLat != null && parsedLon != null &&
        parsedLat in -90.0..90.0 && parsedLon in -180.0..180.0

    val amenityOptions = listOf("restaurant", "fast_food", "cafe", "bar", "pub")
    val priorities = listOf("Low", "Medium", "High")

    fun emptyToNull(value: String): String? = value.trim().ifBlank { null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Custom Venue" else "Add Custom Restaurant",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Saved locally in your personal library with distance sorting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Venue Name *") },
                    placeholder = { Text("e.g. Corner Taqueria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    amenityOptions.forEach { option ->
                        FilterChip(
                            selected = amenity.equals(option, ignoreCase = true),
                            onClick = { amenity = option },
                            label = { Text(option.replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                OutlinedTextField(
                    value = cuisine,
                    onValueChange = { cuisine = it },
                    label = { Text("Cuisine (e.g. mexican, diner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = housenumber,
                        onValueChange = { housenumber = it },
                        label = { Text("No.") },
                        singleLine = true,
                        modifier = Modifier.weight(0.35f)
                    )
                    OutlinedTextField(
                        value = street,
                        onValueChange = { street = it },
                        label = { Text("Street") },
                        singleLine = true,
                        modifier = Modifier.weight(0.65f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        singleLine = true,
                        modifier = Modifier.weight(0.6f)
                    )
                    OutlinedTextField(
                        value = postcode,
                        onValueChange = { postcode = it },
                        label = { Text("ZIP / Postcode") },
                        singleLine = true,
                        modifier = Modifier.weight(0.4f)
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = openingHours,
                    onValueChange = { openingHours = it },
                    label = { Text("Opening Hours (OSM format)") },
                    placeholder = { Text("e.g. Mo-Fr 09:00-21:00; Sa-Su 10:00-22:00") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text("Coordinates", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                OutlinedButton(
                    onClick = {
                        latitudeText = String.format(Locale.US, "%.6f", currentAnchor.latitude)
                        longitudeText = String.format(Locale.US, "%.6f", currentAnchor.longitude)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Use Current GPS / Selected Anchor Coordinates")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = { latitudeText = it },
                        label = { Text("Latitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = parsedLat == null || parsedLat !in -90.0..90.0,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = { longitudeText = it },
                        label = { Text("Longitude") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = parsedLon == null || parsedLon !in -180.0..180.0,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isAlreadyInWantToTry) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add to Want to Try",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Save straight to your Want to Try list",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = addToWantToTry,
                            onCheckedChange = { addToWantToTry = it }
                        )
                    }

                    if (addToWantToTry) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            priorities.forEach { priority ->
                                FilterChip(
                                    selected = wantToTryPriority == priority,
                                    onClick = { wantToTryPriority = priority },
                                    label = { Text(priority) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = wantToTryNotes,
                            onValueChange = { wantToTryNotes = it },
                            label = { Text("Want to Try Notes (optional)") },
                            placeholder = { Text("e.g. Heard great things about tacos") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()

                FlagSelectorRow(
                    label = "Takeaway",
                    selectedValue = takeaway,
                    options = listOf("yes", "only", "no"),
                    onSelect = { takeaway = it }
                )
                FlagSelectorRow(
                    label = "Delivery",
                    selectedValue = delivery,
                    options = listOf("yes", "only", "no"),
                    onSelect = { delivery = it }
                )
                FlagSelectorRow(
                    label = "Outdoor Seating",
                    selectedValue = outdoorSeating,
                    options = listOf("yes", "no"),
                    onSelect = { outdoorSeating = it }
                )
                FlagSelectorRow(
                    label = "Drive-Through",
                    selectedValue = driveThrough,
                    options = listOf("yes", "no"),
                    onSelect = { driveThrough = it }
                )
                FlagSelectorRow(
                    label = "Wheelchair",
                    selectedValue = wheelchair,
                    options = listOf("yes", "limited", "designated", "no"),
                    onSelect = { wheelchair = it }
                )
                FlagSelectorRow(
                    label = "Vegetarian",
                    selectedValue = dietVegetarian,
                    options = listOf("yes", "only", "no"),
                    onSelect = { dietVegetarian = it }
                )
                FlagSelectorRow(
                    label = "Vegan",
                    selectedValue = dietVegan,
                    options = listOf("yes", "only", "no"),
                    onSelect = { dietVegan = it }
                )
                FlagSelectorRow(
                    label = "Gluten-Free",
                    selectedValue = dietGlutenFree,
                    options = listOf("yes", "only", "no"),
                    onSelect = { dietGlutenFree = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = parsedLat ?: currentAnchor.latitude
                    val lon = parsedLon ?: currentAnchor.longitude
                    val custom = CustomRestaurant(
                        id = existingCustomRestaurant?.id ?: CustomRestaurant.generateCustomId(),
                        name = name.trim(),
                        amenity = amenity,
                        cuisine = emptyToNull(cuisine),
                        street = emptyToNull(street),
                        housenumber = emptyToNull(housenumber),
                        postcode = emptyToNull(postcode),
                        city = emptyToNull(city),
                        phone = emptyToNull(phone),
                        website = emptyToNull(website),
                        openingHours = emptyToNull(openingHours),
                        takeaway = takeaway,
                        delivery = delivery,
                        outdoorSeating = outdoorSeating,
                        driveThrough = driveThrough,
                        wheelchair = wheelchair,
                        dietVegetarian = dietVegetarian,
                        dietVegan = dietVegan,
                        dietGlutenFree = dietGlutenFree,
                        latitude = lat,
                        longitude = lon,
                        createdAt = existingCustomRestaurant?.createdAt ?: System.currentTimeMillis()
                    )
                    onSave(custom, addToWantToTry, wantToTryPriority, wantToTryNotes.trim())
                },
                enabled = name.isNotBlank() && coordsValid,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPastelBlue)
            ) {
                Text(if (isEditing) "Save" else "Add Venue")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isEditing && onDelete != null) {
                    TextButton(onClick = { confirmDeleteOpen = true }) {
                        Text("Delete", color = ErrorPastelRed)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        },
        containerColor = DarkSurfaceLevel2
    )

    if (confirmDeleteOpen && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteOpen = false },
            title = { Text("Delete Custom Venue?", color = TextPrimary) },
            text = {
                Text(
                    text = "Remove ${existingCustomRestaurant?.name ?: "this custom venue"} and its saved local entries?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteOpen = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorPastelRed)
                ) {
                    Text("Delete", color = DarkBgBody)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteOpen = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceLevel2
        )
    }
}

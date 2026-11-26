package com.sprillex.restaurantfinder.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantOverride
import com.sprillex.restaurantfinder.ui.theme.BrandPastelBlue
import com.sprillex.restaurantfinder.ui.theme.DarkSurfaceLevel2
import com.sprillex.restaurantfinder.ui.theme.ErrorPastelRed
import com.sprillex.restaurantfinder.ui.theme.TextPrimary
import com.sprillex.restaurantfinder.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditRestaurantDialog(
    baseRestaurant: Restaurant,
    currentOverride: RestaurantOverride?,
    onSave: (RestaurantOverride) -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentOverride?.name ?: baseRestaurant.name) }
    var amenity by remember { mutableStateOf(currentOverride?.amenity ?: baseRestaurant.amenity) }
    var cuisine by remember { mutableStateOf(currentOverride?.cuisine ?: baseRestaurant.cuisine.orEmpty()) }
    var housenumber by remember { mutableStateOf(currentOverride?.housenumber ?: baseRestaurant.housenumber.orEmpty()) }
    var street by remember { mutableStateOf(currentOverride?.street ?: baseRestaurant.street.orEmpty()) }
    var city by remember { mutableStateOf(currentOverride?.city ?: baseRestaurant.city.orEmpty()) }
    var postcode by remember { mutableStateOf(currentOverride?.postcode ?: baseRestaurant.postcode.orEmpty()) }
    var phone by remember { mutableStateOf(currentOverride?.phone ?: baseRestaurant.phone.orEmpty()) }
    var website by remember { mutableStateOf(currentOverride?.website ?: baseRestaurant.website.orEmpty()) }
    var openingHours by remember { mutableStateOf(currentOverride?.openingHours ?: baseRestaurant.opening_hours.orEmpty()) }

    var takeaway by remember { mutableStateOf(currentOverride?.takeaway ?: baseRestaurant.takeaway) }
    var delivery by remember { mutableStateOf(currentOverride?.delivery ?: baseRestaurant.delivery) }
    var outdoorSeating by remember { mutableStateOf(currentOverride?.outdoorSeating ?: baseRestaurant.outdoor_seating) }
    var driveThrough by remember { mutableStateOf(currentOverride?.driveThrough ?: baseRestaurant.drive_through) }
    var wheelchair by remember { mutableStateOf(currentOverride?.wheelchair ?: baseRestaurant.wheelchair) }
    var dietVegetarian by remember { mutableStateOf(currentOverride?.dietVegetarian ?: baseRestaurant.diet_vegetarian) }
    var dietVegan by remember { mutableStateOf(currentOverride?.dietVegan ?: baseRestaurant.diet_vegan) }
    var dietGlutenFree by remember { mutableStateOf(currentOverride?.dietGlutenFree ?: baseRestaurant.diet_gluten_free) }
    var isClosed by remember { mutableStateOf(currentOverride?.isClosed == true) }

    val amenityOptions = listOf("restaurant", "fast_food", "cafe", "bar", "pub")

    fun diffNullable(edited: String, baseVal: String?, existingOverride: String?): String? {
        val trimmed = edited.trim()
        val baseTrimmed = baseVal.orEmpty().trim()
        return when {
            trimmed == baseTrimmed && existingOverride == null -> null
            trimmed == baseTrimmed && existingOverride != null -> null
            else -> trimmed
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Venue Details",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Overrides are stored locally and never modify the base OSM database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mark Venue Closed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isClosed) ErrorPastelRed else TextPrimary
                    )
                    Switch(
                        checked = isClosed,
                        onCheckedChange = { isClosed = it }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Amenity", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
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
                    val trimmedName = name.trim()
                    val override = RestaurantOverride(
                        restaurantId = baseRestaurant.id,
                        name = if (trimmedName.isNotEmpty() && trimmedName != baseRestaurant.name) trimmedName else null,
                        amenity = if (amenity.isNotBlank() && amenity != baseRestaurant.amenity) amenity else null,
                        cuisine = diffNullable(cuisine, baseRestaurant.cuisine, currentOverride?.cuisine),
                        street = diffNullable(street, baseRestaurant.street, currentOverride?.street),
                        housenumber = diffNullable(housenumber, baseRestaurant.housenumber, currentOverride?.housenumber),
                        postcode = diffNullable(postcode, baseRestaurant.postcode, currentOverride?.postcode),
                        city = diffNullable(city, baseRestaurant.city, currentOverride?.city),
                        phone = diffNullable(phone, baseRestaurant.phone, currentOverride?.phone),
                        website = diffNullable(website, baseRestaurant.website, currentOverride?.website),
                        openingHours = diffNullable(openingHours, baseRestaurant.opening_hours, currentOverride?.openingHours),
                        takeaway = if (takeaway != baseRestaurant.takeaway) takeaway else null,
                        delivery = if (delivery != baseRestaurant.delivery) delivery else null,
                        outdoorSeating = if (outdoorSeating != baseRestaurant.outdoor_seating) outdoorSeating else null,
                        driveThrough = if (driveThrough != baseRestaurant.drive_through) driveThrough else null,
                        wheelchair = if (wheelchair != baseRestaurant.wheelchair) wheelchair else null,
                        dietVegetarian = if (dietVegetarian != baseRestaurant.diet_vegetarian) dietVegetarian else null,
                        dietVegan = if (dietVegan != baseRestaurant.diet_vegan) dietVegan else null,
                        dietGlutenFree = if (dietGlutenFree != baseRestaurant.diet_gluten_free) dietGlutenFree else null,
                        isClosed = if (isClosed) true else null,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(override)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPastelBlue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (currentOverride != null) {
                    TextButton(onClick = onResetToDefaults) {
                        Text("Reset to OSM Defaults", color = ErrorPastelRed)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun FlagSelectorRow(
    label: String,
    selectedValue: String?,
    options: List<String>,
    onSelect: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selectedValue == null,
                onClick = { onSelect(null) },
                label = { Text("Unset", style = MaterialTheme.typography.labelSmall) }
            )
            options.forEach { option ->
                FilterChip(
                    selected = selectedValue == option,
                    onClick = { onSelect(option) },
                    label = {
                        Text(
                            option.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }
        }
    }
}

package com.sprillex.restaurantfinder.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sprillex.restaurantfinder.data.PhotoStorageManager
import com.sprillex.restaurantfinder.data.Restaurant
import com.sprillex.restaurantfinder.data.RestaurantPhoto
import com.sprillex.restaurantfinder.data.VisitLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object IntentHelper {
    fun launchNavigation(context: Context, restaurant: Restaurant) {
        val gmmIntentUri = Uri.parse("geo:0,0?q=${restaurant.latitude},${restaurant.longitude}(${Uri.encode(restaurant.name)})")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "No map application available", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchDialer(context: Context, phoneNumber: String) {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
        try {
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch dialer", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchBrowser(context: Context, url: String) {
        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
        try {
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open website", Toast.LENGTH_SHORT).show()
        }
    }

    fun formatShareText(restaurant: Restaurant): String {
        val category = restaurant.cuisine ?: restaurant.amenity.replace('_', ' ')
        val streetLine = listOfNotNull(restaurant.housenumber, restaurant.street)
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val address = listOfNotNull(streetLine.ifBlank { null }, restaurant.city, restaurant.postcode)
            .filter { it.isNotBlank() }
            .joinToString(", ")

        val mapUrl = "https://maps.google.com/?q=${restaurant.latitude},${restaurant.longitude}"

        return buildString {
            append("Check out ").append(restaurant.name).append("!")
            if (category.isNotBlank()) {
                append("\nCategory: ").append(category.replaceFirstChar { it.uppercase() })
            }
            if (address.isNotBlank()) {
                append("\nAddress: ").append(address)
            }
            if (!restaurant.phone.isNullOrBlank()) {
                append("\nPhone: ").append(restaurant.phone)
            }
            if (!restaurant.website.isNullOrBlank()) {
                append("\nWebsite: ").append(restaurant.website)
            }
            append("\nLocation: ").append(mapUrl)
        }
    }

    fun formatVisitLogShareText(
        restaurant: Restaurant,
        visitLog: VisitLog,
        dateLabel: String? = null
    ): String {
        val formattedDate = dateLabel ?: SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(visitLog.visitedAt))
        val clampedRating = visitLog.rating.coerceIn(1, 5)
        val stars = "★".repeat(clampedRating) + "☆".repeat(5 - clampedRating)
        return buildString {
            append("Visit to ").append(restaurant.name)
            append("\nDate: ").append(formattedDate)
            append("\nRating: ").append(stars).append(" ($clampedRating/5)")
            if (visitLog.orderedDishes.isNotBlank()) {
                append("\nOrdered: ").append(visitLog.orderedDishes)
            }
            if (visitLog.notes.isNotBlank()) {
                append("\nNotes: ").append(visitLog.notes)
            }
        }
    }

    fun formatDishShareText(
        restaurant: Restaurant,
        dishName: String,
        notes: String = "",
        isFavorite: Boolean = false
    ): String {
        val label = if (isFavorite) "Favorite dish at" else "Dish to try at"
        return buildString {
            append("$label ${restaurant.name}: $dishName")
            if (notes.isNotBlank()) {
                append("\nNotes: ").append(notes)
            }
        }
    }

    fun shareRestaurant(context: Context, restaurant: Restaurant) {
        launchShareTextIntent(
            context = context,
            subject = restaurant.name,
            text = formatShareText(restaurant),
            chooserTitle = "Share ${restaurant.name} via",
            errorMsg = "Unable to share restaurant"
        )
    }

    fun shareVisitLog(context: Context, restaurant: Restaurant, visitLog: VisitLog) {
        launchShareTextIntent(
            context = context,
            subject = "Visit to ${restaurant.name}",
            text = formatVisitLogShareText(restaurant, visitLog),
            chooserTitle = "Share visit log via",
            errorMsg = "Unable to share visit log"
        )
    }

    fun shareDish(
        context: Context,
        restaurant: Restaurant,
        dishName: String,
        notes: String = "",
        isFavorite: Boolean = false
    ) {
        launchShareTextIntent(
            context = context,
            subject = "$dishName at ${restaurant.name}",
            text = formatDishShareText(restaurant, dishName, notes, isFavorite),
            chooserTitle = "Share dish via",
            errorMsg = "Unable to share dish"
        )
    }

    fun sharePhoto(context: Context, restaurant: Restaurant, photo: RestaurantPhoto) {
        val uri = PhotoStorageManager.getShareablePhotoUri(context, photo.relativePath)
        if (uri == null) {
            Toast.makeText(context, "Photo file unavailable", Toast.LENGTH_SHORT).show()
            return
        }
        sharePhotoUri(
            context = context,
            photoUri = uri,
            restaurantName = restaurant.name,
            category = photo.category,
            caption = photo.caption
        )
    }

    fun sharePhotoUri(
        context: Context,
        photoUri: Uri,
        restaurantName: String,
        category: String = "",
        caption: String = ""
    ) {
        val textSummary = buildString {
            append(restaurantName)
            if (category.isNotBlank()) append(" ($category)")
            if (caption.isNotBlank()) append(" — $caption")
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, photoUri)
            putExtra(Intent.EXTRA_SUBJECT, "$restaurantName photo")
            putExtra(Intent.EXTRA_TEXT, textSummary)
            clipData = ClipData.newUri(context.contentResolver, "Photo", photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share photo via").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchShareTextIntent(
        context: Context,
        subject: String,
        text: String,
        chooserTitle: String,
        errorMsg: String
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle)
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }
}

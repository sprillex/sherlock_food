package com.sprillex.restaurantfinder.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.sprillex.restaurantfinder.data.Restaurant

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

    fun shareRestaurant(context: Context, restaurant: Restaurant) {
        val shareText = formatShareText(restaurant)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, restaurant.name)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(shareIntent, "Share ${restaurant.name} via")
        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share restaurant", Toast.LENGTH_SHORT).show()
        }
    }
}

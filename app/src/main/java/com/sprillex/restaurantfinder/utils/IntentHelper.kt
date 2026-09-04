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
}

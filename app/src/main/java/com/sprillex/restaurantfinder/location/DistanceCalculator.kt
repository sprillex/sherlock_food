package com.sprillex.restaurantfinder.location

import kotlin.math.*

object DistanceCalculator {
    private const val EARTH_RADIUS_MILES = 3958.8

    /**
     * Calculates the distance between two geographic coordinates using the Haversine formula.
     * Returns distance in miles.
     */
    fun calculateDistanceMiles(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_MILES * c
    }

    /**
     * Formats distance into human-readable string (e.g. "850 ft", "0.4 mi", "12.3 mi").
     */
    fun formatDistance(distanceMiles: Double): String {
        return if (distanceMiles < 0.2) {
            val feet = (distanceMiles * 5280).toInt()
            "$feet ft"
        } else if (distanceMiles < 10.0) {
            String.format("%.1f mi", distanceMiles)
        } else {
            String.format("%.0f mi", distanceMiles)
        }
    }
}

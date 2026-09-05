package com.sprillex.restaurantfinder.location

data class AnchorLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object LocationManager {
    val PRESET_ANCHORS = listOf(
        AnchorLocation("Toledo, OH", 41.6528, -83.5379),
        AnchorLocation("Perrysburg, OH", 41.5570, -83.6272),
        AnchorLocation("Monroe, MI", 41.9164, -83.3977),
        AnchorLocation("Adrian, MI", 41.8975, -84.0372)
    )

    val DEFAULT_ANCHOR = PRESET_ANCHORS[0]
}

package com.sprillex.restaurantfinder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = BrandPastelBlue,
    background = DarkBgBody,
    surface = DarkSurfaceLevel1,
    surfaceVariant = DarkSurfaceLevel2,
    onPrimary = DarkBgBody,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorPastelRed,
    outline = BorderDark
)

@Composable
fun RestaurantFinderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Mandated Robust Dark Mode UI specification
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

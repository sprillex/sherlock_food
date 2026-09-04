package com.sprillex.restaurantfinder.ui.theme

import androidx.compose.ui.graphics.Color

// Robust Dark Mode Palette (AI_MANUAL_DESIGN_STANDARDS.md compliant)
// Base Background: Dark Grey #121212 (Not #000000 pure black to prevent OLED smearing)
val DarkBgBody = Color(0xFF121212)

// Surface Elevations (Lightness indicates closeness)
val DarkSurfaceLevel1 = Color(0xFF1E1E1E) // Cards & Containers
val DarkSurfaceLevel2 = Color(0xFF2D2D2D) // Modals & Bottom Sheets

// Typography Hierarchy (White with opacity)
val TextPrimary = Color(0xDEFFFFFF)   // 87% Opacity
val TextSecondary = Color(0x99FFFFFF) // 60% Opacity
val TextDisabled = Color(0x61FFFFFF)  // 38% Opacity

// Desaturated Accents
val BrandPastelBlue = Color(0xFF8AB4F8)
val ErrorPastelRed = Color(0xFFF28B82)
val BorderDark = Color(0xFF333333)

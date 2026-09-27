package com.example.vehiclemaintenance.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The app's own primary and popup colors, applied by hand where the owner asked for them instead
 * of replacing the Material scheme roles that every other component reads.
 */
@Immutable
data class BrandColors(
    val primary: Color,
    val onPrimary: Color,
    val popupContainer: Color,
)

val LightBrandColors = BrandColors(
    primary = PrimaryColorLightMode,
    onPrimary = Color.White,
    popupContainer = PopupColorLightMode,
)

val DarkBrandColors = BrandColors(
    primary = PrimaryColorDarkMode,
    onPrimary = Color.White,
    popupContainer = PopupColorDarkMode,
)

val LocalBrandColors = staticCompositionLocalOf { LightBrandColors }

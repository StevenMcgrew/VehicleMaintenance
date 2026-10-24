package com.example.vehiclemaintenance.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Pie chart colors, which Material 3 has no roles for. [slices] cycle across tracked items, and
 * Additional repairs takes a soft grey so it reads as a catch-all and never matches one of them.
 */
@Immutable
data class ChartColors(
    val slices: List<Color>,
    val additionalRepairs: Color,
)

private val PastelSlices = listOf(
    PastelIcyBlue,
    PastelRoseGold,
    PastelGoldBlossom,
    PastelCelery,
    PastelLavender,
    PastelSeafoam,
    PastelPink,
    PastelPeach,
)

// Pastels sit softly on a light surface and stand out on a dark one, so both themes share them.
val LightChartColors = ChartColors(slices = PastelSlices, additionalRepairs = PastelGrey)

val DarkChartColors = ChartColors(slices = PastelSlices, additionalRepairs = PastelGrey)

val LocalChartColors = staticCompositionLocalOf { LightChartColors }

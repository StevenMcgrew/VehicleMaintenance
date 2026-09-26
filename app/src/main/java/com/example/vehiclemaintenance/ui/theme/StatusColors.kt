package com.example.vehiclemaintenance.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Traffic-light colors for maintenance status text, which Material 3 has no roles for. */
@Immutable
data class StatusColors(
    val overdue: Color,
    val due: Color,
    val ok: Color,
)

val LightStatusColors = StatusColors(overdue = Red45, due = Amber45, ok = Green45)

val DarkStatusColors = StatusColors(overdue = Red60, due = Amber70, ok = Green70)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

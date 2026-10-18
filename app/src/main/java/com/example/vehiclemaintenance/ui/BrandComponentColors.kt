package com.example.vehiclemaintenance.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.ui.theme.LocalBrandColors

/** A filled button on the brand primary with white content. Disabled colors stay Material's. */
@Composable
fun brandButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = LocalBrandColors.current.primary,
    contentColor = LocalBrandColors.current.onPrimary,
)

/** A text button whose label is the brand primary. */
@Composable
fun brandTextButtonColors(): ButtonColors = ButtonDefaults.textButtonColors(
    contentColor = LocalBrandColors.current.primary,
)

/** An outlined button whose label is the brand primary; pair it with [brandOutlinedButtonBorder]. */
@Composable
fun brandOutlinedButtonColors(): ButtonColors = ButtonDefaults.outlinedButtonColors(
    contentColor = LocalBrandColors.current.primary,
)

/** The brand primary outline, faded like Material's when the button is disabled. */
@Composable
fun brandOutlinedButtonBorder(enabled: Boolean): BorderStroke {
    val primary = LocalBrandColors.current.primary
    return BorderStroke(1.dp, if (enabled) primary else primary.copy(alpha = 0.12f))
}

/** The disabled icon keeps Material's dimmed alpha, which a plain Icon tint would lose. */
@Composable
fun brandIconButtonColors(): IconButtonColors = IconButtonDefaults.iconButtonColors(
    contentColor = LocalBrandColors.current.primary,
)

/** A menu item whose label is the brand primary, the app's cue for tappable text. */
@Composable
fun brandMenuItemColors(): MenuItemColors = MenuDefaults.itemColors(
    textColor = LocalBrandColors.current.primary,
)

/** The selected segment is filled like a brand button; the others are outlined like one. */
@Composable
fun brandSegmentedButtonColors(): SegmentedButtonColors {
    val brand = LocalBrandColors.current
    return SegmentedButtonDefaults.colors(
        activeContainerColor = brand.primary,
        activeContentColor = brand.onPrimary,
        activeBorderColor = brand.primary,
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = brand.primary,
        inactiveBorderColor = brand.primary,
    )
}

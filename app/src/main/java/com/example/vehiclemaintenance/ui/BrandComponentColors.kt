package com.example.vehiclemaintenance.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
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

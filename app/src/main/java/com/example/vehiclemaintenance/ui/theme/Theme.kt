package com.example.vehiclemaintenance.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    inversePrimary = Blue40,
    secondary = BlueGrey80,
    onSecondary = BlueGrey20,
    secondaryContainer = BlueGrey30,
    onSecondaryContainer = BlueGrey90,
    tertiary = Azure80,
    onTertiary = Azure20,
    tertiaryContainer = Azure30,
    onTertiaryContainer = Azure90,
    error = Red60,
    onError = Red10,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Grey6,
    onBackground = Grey90,
    surface = Grey6,
    onSurface = Grey90,
    surfaceVariant = SlateGrey30,
    onSurfaceVariant = SlateGrey80,
    surfaceTint = Blue80,
    inverseSurface = Grey90,
    inverseOnSurface = Grey20,
    outline = SlateGrey60,
    outlineVariant = SlateGrey30,
    surfaceBright = Grey24,
    surfaceDim = Grey6,
    surfaceContainerLowest = Grey4,
    surfaceContainerLow = Grey10,
    surfaceContainer = Grey12,
    surfaceContainerHigh = Grey17,
    surfaceContainerHighest = Grey22,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    inversePrimary = Blue80,
    secondary = BlueGrey40,
    onSecondary = Color.White,
    secondaryContainer = BlueGrey90,
    onSecondaryContainer = BlueGrey10,
    tertiary = Azure40,
    onTertiary = Color.White,
    tertiaryContainer = Azure90,
    onTertiaryContainer = Azure10,
    error = Red45,
    onError = Color.White,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Grey98,
    onBackground = Grey10,
    surface = Grey98,
    onSurface = Grey10,
    surfaceVariant = SlateGrey90,
    onSurfaceVariant = SlateGrey30,
    surfaceTint = Blue40,
    inverseSurface = Grey20,
    inverseOnSurface = Grey95,
    outline = SlateGrey50,
    outlineVariant = SlateGrey80,
    surfaceBright = Grey98,
    surfaceDim = Grey87,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Grey96,
    surfaceContainer = Grey94,
    surfaceContainerHigh = Grey92,
    surfaceContainerHighest = Grey90,
)

@Composable
fun VehicleMaintenanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+, but it would replace the app's blue palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

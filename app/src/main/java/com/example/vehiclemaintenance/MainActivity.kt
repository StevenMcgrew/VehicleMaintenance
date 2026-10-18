package com.example.vehiclemaintenance

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.vehiclemaintenance.settings.ThemeViewModel
import com.example.vehiclemaintenance.settings.isDark
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels { ThemeViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by themeViewModel.themeMode.collectAsState()
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())

            // The default edge-to-edge styles follow the system, not the in-app theme choice.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            VehicleMaintenanceTheme(darkTheme = darkTheme) {
                VehicleMaintenanceApp(
                    themeMode = themeMode,
                    onThemeModeChange = themeViewModel::setThemeMode,
                )
            }
        }
    }

    private companion object {
        // The same scrims enableEdgeToEdge uses by default for three-button navigation.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}

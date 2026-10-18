package com.example.vehiclemaintenance.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Device preferences. Kept out of the JSON store on purpose: they are not vehicle history and
 * must not travel in export files.
 */
interface SettingsRepository {
    val themeMode: StateFlow<ThemeMode>

    suspend fun load()

    suspend fun setThemeMode(mode: ThemeMode)
}

/** Opening the preferences file reads disk, so [preferences] is only called on [ioDispatcher]. */
class SharedPreferencesSettingsRepository(
    private val preferences: () -> SharedPreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SettingsRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun load() {
        _themeMode.value = withContext(ioDispatcher) {
            themeModeFromStored(preferences().getString(THEME_MODE_KEY, null))
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        withContext(ioDispatcher) {
            preferences().edit { putString(THEME_MODE_KEY, mode.name) }
        }
    }

    companion object {
        const val PREFERENCES_NAME = "settings"
        private const val THEME_MODE_KEY = "theme_mode"
    }
}

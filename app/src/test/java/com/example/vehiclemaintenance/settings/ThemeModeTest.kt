package com.example.vehiclemaintenance.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun systemFollowsTheDevice() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemIsDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemIsDark = false))
    }

    @Test
    fun lightIgnoresADarkDevice() {
        assertFalse(ThemeMode.LIGHT.isDark(systemIsDark = true))
    }

    @Test
    fun darkIgnoresALightDevice() {
        assertTrue(ThemeMode.DARK.isDark(systemIsDark = false))
    }

    @Test
    fun storedNameRoundTrips() {
        ThemeMode.entries.forEach { assertEquals(it, themeModeFromStored(it.name)) }
    }

    @Test
    fun missingOrUnknownStoredValueFollowsTheSystem() {
        assertEquals(ThemeMode.SYSTEM, themeModeFromStored(null))
        assertEquals(ThemeMode.SYSTEM, themeModeFromStored("SEPIA"))
    }
}

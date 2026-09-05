package com.example.vehiclemaintenance.maintenance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MileageFormatTest {

    @Test
    fun groupsThousands() {
        assertEquals("5,000", formatMileage(5_000, Locale.US))
    }

    @Test
    fun groupsSixFigureValues() {
        assertEquals("120,000", formatMileage(120_000, Locale.US))
    }

    @Test
    fun leavesShortValuesAlone() {
        assertEquals("750", formatMileage(750, Locale.US))
    }

    @Test
    fun usesTheSeparatorOfTheGivenLocale() {
        assertEquals("5.000", formatMileage(5_000, Locale.GERMANY))
    }
}

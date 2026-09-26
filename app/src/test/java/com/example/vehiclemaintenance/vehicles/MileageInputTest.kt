package com.example.vehiclemaintenance.vehicles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MileageInputTest {

    @Test
    fun `blank and whitespace are blank`() {
        assertEquals(MileageInput.Blank, parseMileage(""))
        assertEquals(MileageInput.Blank, parseMileage("   "))
    }

    @Test
    fun `a whole number of zero or more is miles, trimmed`() {
        assertEquals(MileageInput.Miles(0), parseMileage("0"))
        assertEquals(MileageInput.Miles(45000), parseMileage(" 45000 "))
    }

    @Test
    fun `text, decimals, and negatives are invalid`() {
        assertEquals(MileageInput.Invalid, parseMileage("lots"))
        assertEquals(MileageInput.Invalid, parseMileage("45000.5"))
        assertEquals(MileageInput.Invalid, parseMileage("-1"))
        assertEquals(MileageInput.Invalid, parseMileage("45,000"))
    }

    @Test
    fun `only a reading below the highest known one warns`() {
        assertTrue(needsLowerMileageWarning(44_999, highestKnown = 45_000))
        assertFalse(needsLowerMileageWarning(45_000, highestKnown = 45_000))
        assertFalse(needsLowerMileageWarning(45_001, highestKnown = 45_000))
        assertFalse(needsLowerMileageWarning(0, highestKnown = null))
    }
}

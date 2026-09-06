package com.example.vehiclemaintenance.maintenance

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DateFormatTest {

    private val date = LocalDate.of(2027, 3, 15)

    @Test
    fun shortDatesAreNumericSoTheyFitAColumn() {
        assertEquals("3/15/27", formatShortDate(date, Locale.US))
    }

    @Test
    fun mediumDatesNameTheMonth() {
        assertEquals("Mar 15, 2027", formatMediumDate(date, Locale.US))
    }

    @Test
    fun bothFormatsFollowTheGivenLocale() {
        assertEquals("15.03.27", formatShortDate(date, Locale.GERMANY))
        assertEquals("15.03.2027", formatMediumDate(date, Locale.GERMANY))
    }
}

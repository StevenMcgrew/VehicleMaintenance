package com.example.vehiclemaintenance.servicelog

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class CostFormatTest {

    @Test
    fun `formats zero minor units as a whole amount`() {
        assertEquals("$0.00", formatCost(0, Locale.US))
    }

    @Test
    fun `formats minor units as major units and cents`() {
        assertEquals("$64.99", formatCost(6499, Locale.US))
    }

    @Test
    fun `groups a four figure amount`() {
        assertEquals("$1,234.56", formatCost(123456, Locale.US))
    }

    @Test
    fun `formats a long total larger than an Int can hold`() {
        assertEquals("$42,949,672.96", formatCost(4_294_967_296L, Locale.US))
    }
}

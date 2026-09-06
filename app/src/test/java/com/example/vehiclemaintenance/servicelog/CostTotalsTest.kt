package com.example.vehiclemaintenance.servicelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CostTotalsTest {

    private var nextId = 0

    private fun entry(year: Int, cost: Int?, month: Int = 6): ServiceLogEntry = ServiceLogEntry(
        id = "e-${nextId++}",
        vehicleId = "v-1",
        description = "Oil change",
        date = LocalDate.of(year, month, 15),
        odometer = 50_000,
        cost = cost,
    )

    @Test
    fun `no entries produce a zero total and no year rows`() {
        val totals = costTotalsOf(emptyList())

        assertEquals(0L, totals.allTime)
        assertTrue(totals.byYear.isEmpty())
    }

    @Test
    fun `entries without a cost are not counted and create no year row`() {
        val totals = costTotalsOf(listOf(entry(2025, null), entry(2024, null)))

        assertEquals(0L, totals.allTime)
        assertTrue(totals.byYear.isEmpty())
    }

    @Test
    fun `a null cost is skipped while its costed neighbours still sum`() {
        val totals = costTotalsOf(
            listOf(entry(2026, 6499), entry(2026, null), entry(2026, 1501)),
        )

        assertEquals(8000L, totals.allTime)
        assertEquals(listOf(YearCost(2026, 8000L)), totals.byYear)
    }

    @Test
    fun `a year whose only entry has no cost is left out of the breakdown`() {
        val totals = costTotalsOf(
            listOf(entry(2026, 2500), entry(2025, null), entry(2024, 7500)),
        )

        assertEquals(10_000L, totals.allTime)
        assertEquals(listOf(YearCost(2026, 2500L), YearCost(2024, 7500L)), totals.byYear)
    }

    @Test
    fun `years are ordered newest first regardless of entry order`() {
        val totals = costTotalsOf(
            listOf(entry(2024, 100), entry(2026, 200), entry(2025, 300)),
        )

        assertEquals(listOf(2026, 2025, 2024), totals.byYear.map { it.year })
        assertEquals(600L, totals.allTime)
    }

    @Test
    fun `entries in the same year are grouped into one row`() {
        val totals = costTotalsOf(
            listOf(entry(2026, 1000, month = 1), entry(2026, 2000, month = 11)),
        )

        assertEquals(listOf(YearCost(2026, 3000L)), totals.byYear)
    }

    @Test
    fun `a history larger than Int MAX_VALUE minor units does not overflow`() {
        val large = Int.MAX_VALUE
        val totals = costTotalsOf(listOf(entry(2026, large), entry(2025, large)))

        assertEquals(2L * large, totals.allTime)
        assertEquals(large.toLong(), totals.byYear.first().total)
    }

    @Test
    fun `all time equals the sum of the year rows`() {
        val totals = costTotalsOf(
            listOf(entry(2026, 1234), entry(2025, 5678), entry(2025, 9), entry(2024, null)),
        )

        assertEquals(totals.byYear.sumOf { it.total }, totals.allTime)
    }
}

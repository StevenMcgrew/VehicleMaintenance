package com.example.vehiclemaintenance.servicelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CostTotalsTest {

    private var nextId = 0

    private fun entry(
        year: Int,
        cost: Int?,
        month: Int = 6,
        day: Int = 15,
    ): ServiceLogEntry = ServiceLogEntry(
        id = "e-${nextId++}",
        vehicleId = "v-1",
        description = "Oil change",
        date = LocalDate.of(year, month, day),
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
        assertEquals(listOf(2026 to 8000L), totals.yearRows())
    }

    @Test
    fun `a year whose only entry has no cost is left out of the breakdown`() {
        val totals = costTotalsOf(
            listOf(entry(2026, 2500), entry(2025, null), entry(2024, 7500)),
        )

        assertEquals(10_000L, totals.allTime)
        assertEquals(listOf(2026 to 2500L, 2024 to 7500L), totals.yearRows())
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

        assertEquals(listOf(2026 to 3000L), totals.yearRows())
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

    @Test
    fun `each year lists only its costed entries`() {
        val costed2026 = entry(2026, 1000)
        val uncosted2026 = entry(2026, null)
        val costed2025 = entry(2025, 2000)

        val totals = costTotalsOf(listOf(costed2026, uncosted2026, costed2025))

        assertEquals(listOf(costed2026), totals.byYear[0].entries)
        assertEquals(listOf(costed2025), totals.byYear[1].entries)
    }

    @Test
    fun `a year's entries read newest first`() {
        val march = entry(2026, 100, month = 3)
        val november = entry(2026, 200, month = 11)
        val july = entry(2026, 300, month = 7)

        val totals = costTotalsOf(listOf(march, november, july))

        assertEquals(listOf(november, july, march), totals.byYear.single().entries)
    }

    @Test
    fun `entries sharing a date keep the order they were given in`() {
        val first = entry(2026, 100, month = 9, day = 5)
        val second = entry(2026, 200, month = 9, day = 5)

        val totals = costTotalsOf(listOf(first, second))

        assertEquals(listOf(first, second), totals.byYear.single().entries)
    }

    private fun VehicleCostTotals.yearRows(): List<Pair<Int, Long>> = byYear.map { it.year to it.total }
}

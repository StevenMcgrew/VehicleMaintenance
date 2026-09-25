package com.example.vehiclemaintenance.servicelog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ServiceHistoryTest {

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

    private fun historyOf(vararg entries: ServiceLogEntry) = serviceHistoryOf(entries.toList())

    private fun on(date: String, cost: Int?): ServiceLogEntry {
        val parsed = LocalDate.parse(date)
        return entry(parsed.year, cost, parsed.monthValue, parsed.dayOfMonth)
    }

    @Test
    fun `no entries produce no years and no totals`() {
        val history = historyOf()

        assertTrue(history.years.isEmpty())
        assertNull(history.allTime)
        assertNull(history.averagePerYear)
    }

    @Test
    fun `entries without a cost are listed but produce no totals`() {
        val uncosted = entry(2026, null)

        val history = historyOf(uncosted)

        assertEquals(listOf(uncosted), history.years.single().entries)
        assertNull(history.years.single().total)
        assertNull(history.allTime)
        assertNull(history.averagePerYear)
    }

    @Test
    fun `a null cost is skipped while its costed neighbours still sum`() {
        val history = historyOf(entry(2026, 6499), entry(2026, null), entry(2026, 1501))

        assertEquals(8000L, history.allTime)
        assertEquals(8000L, history.years.single().total)
        assertEquals(3, history.years.single().entries.size)
    }

    @Test
    fun `a year whose entries all lack a cost still gets a section without a subtotal`() {
        val history = historyOf(entry(2026, 2500), entry(2025, null), entry(2024, 7500))

        assertEquals(listOf(2026, 2025, 2024), history.years.map { it.year })
        assertEquals(listOf(2500L, null, 7500L), history.years.map { it.total })
        assertEquals(10_000L, history.allTime)
    }

    @Test
    fun `years are ordered newest first regardless of entry order`() {
        val history = historyOf(entry(2024, 100), entry(2026, 200), entry(2025, 300))

        assertEquals(listOf(2026, 2025, 2024), history.years.map { it.year })
    }

    @Test
    fun `a year's entries read newest first`() {
        val march = entry(2026, 100, month = 3)
        val november = entry(2026, null, month = 11)
        val july = entry(2026, 300, month = 7)

        val history = historyOf(march, november, july)

        assertEquals(listOf(november, july, march), history.years.single().entries)
    }

    @Test
    fun `entries sharing a date keep the order they were given in`() {
        val first = entry(2026, 100, month = 9, day = 5)
        val second = entry(2026, 200, month = 9, day = 5)

        val history = historyOf(first, second)

        assertEquals(listOf(first, second), history.years.single().entries)
    }

    @Test
    fun `a history larger than Int MAX_VALUE minor units does not overflow`() {
        val large = Int.MAX_VALUE
        val history = historyOf(entry(2026, large), entry(2025, large))

        assertEquals(2L * large, history.allTime)
        assertEquals(large.toLong(), history.years.first().total)
    }

    @Test
    fun `all time equals the sum of the year subtotals`() {
        val history = historyOf(
            entry(2026, 1234),
            entry(2025, 5678),
            entry(2025, 9),
            entry(2024, null),
        )

        assertEquals(history.years.sumOf { it.total ?: 0L }, history.allTime)
    }

    @Test
    fun `the average is the monthly cost between the first and last entry times 12`() {
        // 2022-01-01 to 2026-01-01 is 1461 days, exactly four years of 365.25 days.
        val history = historyOf(on("2026-01-01", 30_000), on("2022-01-01", 10_000))

        assertEquals(10_000L, history.averagePerYear)
    }

    @Test
    fun `a partial month counts toward the span instead of being rounded away`() {
        // 182 days is 5.98 months, so $60.00 comes to 60 / 5.98 x 12 = $120.41 a year.
        val history = historyOf(on("2026-07-02", 3_000), on("2026-01-01", 3_000))

        assertEquals(12_041L, history.averagePerYear)
    }

    @Test
    fun `an uncosted entry still widens the span`() {
        val history = historyOf(on("2026-01-01", 10_000), on("2022-01-01", null))

        assertEquals(2_500L, history.averagePerYear)
    }

    @Test
    fun `the average rounds half up to the nearest cent`() {
        // Over exactly four years the average is a quarter of the total.
        assertEquals(1L, historyOf(on("2026-01-01", 2), on("2022-01-01", null)).averagePerYear)
        assertEquals(0L, historyOf(on("2026-01-01", 1), on("2022-01-01", null)).averagePerYear)
        assertEquals(2L, historyOf(on("2026-01-01", 6), on("2022-01-01", null)).averagePerYear)
    }

    @Test
    fun `a span shorter than the minimum gives no average`() {
        val history = historyOf(on("2026-01-30", 5_000), on("2026-01-01", 5_000))

        assertEquals(10_000L, history.allTime)
        assertNull(history.averagePerYear)
    }

    @Test
    fun `a single date gives no average`() {
        assertNull(historyOf(on("2026-03-01", 5_000), on("2026-03-01", 1_000)).averagePerYear)
    }

    @Test
    fun `a span of exactly the minimum has an average`() {
        // 30 days is 0.99 months, so $100.00 comes to $1,217.50 a year.
        val history = historyOf(on("2026-01-31", 10_000), on("2026-01-01", null))

        assertEquals(121_750L, history.averagePerYear)
    }
}

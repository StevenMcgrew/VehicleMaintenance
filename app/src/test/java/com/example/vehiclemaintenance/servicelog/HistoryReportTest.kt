package com.example.vehiclemaintenance.servicelog

import com.example.vehiclemaintenance.vehicles.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class HistoryReportTest {

    private val generatedOn = LocalDate.of(2026, 10, 2)

    private fun entry(
        id: String,
        date: String,
        cost: Int?,
        notes: String? = null,
        description: String = "Oil change",
        itemId: String? = null,
    ) = ServiceLogEntry(
        id = id,
        vehicleId = "v-1",
        maintenanceItemId = itemId,
        description = description,
        date = LocalDate.parse(date),
        odometer = 48_000,
        cost = cost,
        notes = notes,
    )

    private fun reportOf(vararg entries: ServiceLogEntry) = historyReportOf(
        vehicleName = "2020 Ford Ranger 3.0L",
        history = serviceHistoryOf(entries.toList()),
        generatedOn = generatedOn,
        locale = Locale.US,
    )

    @Test
    fun `a costed history carries its totals, years, and rows newest first`() {
        val report = reportOf(
            entry("a", "2025-03-01", cost = 21_000, description = "Brake pads"),
            entry("b", "2026-09-05", cost = 6_499),
            entry("c", "2026-06-12", cost = 78_250, description = "Replaced the alternator"),
        )

        assertEquals("2020 Ford Ranger 3.0L", report.vehicleName)
        assertEquals("Oct 2, 2026", report.generatedOn)
        assertEquals("$1,057.49", report.allTime)
        assertEquals(listOf(2026, 2025), report.years.map { it.year })
        assertEquals("$847.49", report.years[0].total)
        assertEquals(
            listOf("Oil change", "Replaced the alternator"),
            report.years[0].rows.map { it.description },
        )
        val first = report.years[0].rows[0]
        assertEquals("Sep 5, 2026", first.date)
        assertEquals("48,000", first.odometer)
        assertEquals("$64.99", first.cost)
    }

    @Test
    fun `an uncosted history has no totals and no average`() {
        val report = reportOf(entry("a", "2025-03-01", cost = null), entry("b", "2026-03-01", cost = null))

        assertNull(report.allTime)
        assertNull(report.averagePerYear)
        report.years.forEach { assertNull(it.total) }
    }

    @Test
    fun `the average per year is formatted when the history has one`() {
        // Four years is exactly 1461 days, so $400 over them averages to an even $100.
        val report = reportOf(entry("a", "2022-10-02", cost = 20_000), entry("b", "2026-10-02", cost = 20_000))

        assertEquals("$100.00", report.averagePerYear)
    }

    @Test
    fun `notes are kept in full and blank notes are dropped`() {
        val longNote = "Shop said the belts look fine. ".repeat(20).trim()
        val report = reportOf(
            entry("a", "2026-09-05", cost = 6_499, notes = longNote),
            entry("b", "2026-06-12", cost = 6_499, notes = "   "),
        )

        assertEquals(longNote, report.years[0].rows[0].notes)
        assertNull(report.years[0].rows[1].notes)
    }

    @Test
    fun `a missing cost leaves the row's cost empty`() {
        val report = reportOf(entry("a", "2026-03-01", cost = null), entry("b", "2026-04-01", cost = 1_000))

        assertNull(report.years[0].rows.single { it.cost == null }.cost)
        assertEquals("$10.00", report.years[0].total)
    }

    @Test
    fun `the breakdown carries formatted costs, percents, and drawing fractions`() {
        val report = reportOf(
            entry("a", "2026-09-05", cost = 7_500, itemId = "m-1"),
            entry("b", "2025-09-05", cost = 7_500, itemId = "m-1"),
            entry("c", "2026-06-12", cost = 5_000, description = "Replaced the alternator"),
        )

        assertEquals(
            listOf(
                ReportSlice(name = "Oil change", cost = "$150.00", percent = 75, fraction = 0.75f),
                ReportSlice(name = null, cost = "$50.00", percent = 25, fraction = 0.25f),
            ),
            report.breakdown,
        )
    }

    @Test
    fun `an uncosted history has no breakdown`() {
        val report = reportOf(entry("a", "2026-03-01", cost = null, itemId = "m-1"))

        assertTrue(report.breakdown.isEmpty())
    }

    @Test
    fun `the file name is a lower case slug of the vehicle and date`() {
        val vehicle = Vehicle("v-1", 2020, "Ford", "Ranger", "3.0L")

        assertEquals(
            "service-history-2020-ford-ranger-2026-10-02.pdf",
            historyPdfFileName(vehicle, generatedOn),
        )
    }

    @Test
    fun `punctuation and spaces in make and model collapse to single hyphens`() {
        val vehicle = Vehicle("v-1", 2018, "Mercedes-Benz ", "C 300 / 4MATIC!", "2.0L")

        assertEquals(
            "service-history-2018-mercedes-benz-c-300-4matic-2026-10-02.pdf",
            historyPdfFileName(vehicle, generatedOn),
        )
    }
}

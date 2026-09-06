package com.example.vehiclemaintenance.maintenance

import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class MaintenanceStatusTest {

    private val today = LocalDate.of(2026, 9, 6)

    private fun item(
        mileageInterval: Int? = null,
        recurrence: Interval? = null,
        reminder: Interval = Interval(5, IntervalUnit.MONTHS),
        lastDoneDate: LocalDate? = LocalDate.of(2026, 3, 1),
        lastDoneMileage: Int? = null,
    ) = MaintenanceItem(
        id = "m-1",
        vehicleId = "v-1",
        name = "Oil change",
        mileageInterval = mileageInterval,
        recurrence = recurrence,
        reminder = reminder,
        lastDoneDate = lastDoneDate,
        lastDoneMileage = lastDoneMileage,
    )

    @Test
    fun anItemWithTimeLeftOnEveryClockIsOk() {
        val status = statusOf(
            item(recurrence = Interval(1, IntervalUnit.YEARS)),
            currentOdometer = null,
            today = LocalDate.of(2026, 4, 1),
        )

        assertEquals(MaintenanceStatus.OK, status.status)
        assertEquals(LocalDate.of(2026, 8, 1), status.nextReminderDate)
        assertEquals(LocalDate.of(2027, 3, 1), status.dueDate)
    }

    @Test
    fun aPassedReminderIsDueWhileTheDueDateIsStillAhead() {
        val status = statusOf(
            item(recurrence = Interval(1, IntervalUnit.YEARS)),
            currentOdometer = null,
            today = today,
        )

        assertEquals(MaintenanceStatus.DUE, status.status)
    }

    @Test
    fun theReminderCountsOnTheDayItLands() {
        val status = statusOf(item(), currentOdometer = null, today = LocalDate.of(2026, 8, 1))

        assertEquals(MaintenanceStatus.DUE, status.status)
    }

    @Test
    fun aPassedDueDateIsOverdue() {
        val status = statusOf(
            item(recurrence = Interval(3, IntervalUnit.MONTHS)),
            currentOdometer = null,
            today = today,
        )

        assertEquals(MaintenanceStatus.OVERDUE, status.status)
    }

    @Test
    fun theDueDateCountsOnTheDayItLands() {
        val status = statusOf(
            item(recurrence = Interval(3, IntervalUnit.MONTHS)),
            currentOdometer = null,
            today = LocalDate.of(2026, 6, 1),
        )

        assertEquals(MaintenanceStatus.OVERDUE, status.status)
    }

    @Test
    fun aMileageOnlyItemTurnsDueWhenItsReminderArrives() {
        val status = statusOf(
            item(mileageInterval = 7_500, lastDoneMileage = 42_000),
            currentOdometer = 44_000,
            today = today,
        )

        assertEquals(MaintenanceStatus.DUE, status.status)
        assertEquals(49_500, status.mileageDue)
        assertEquals(5_500, status.milesLeft)
    }

    @Test
    fun readingPastTheMileageDueIsOverdue() {
        val status = statusOf(
            item(mileageInterval = 5_000, lastDoneMileage = 42_000, reminder = Interval(2, IntervalUnit.YEARS)),
            currentOdometer = 48_000,
            today = today,
        )

        assertEquals(MaintenanceStatus.OVERDUE, status.status)
        assertEquals(-1_000, status.milesLeft)
    }

    @Test
    fun theMileageDueCountsOnTheExactMile() {
        val status = statusOf(
            item(mileageInterval = 5_000, lastDoneMileage = 42_000, reminder = Interval(2, IntervalUnit.YEARS)),
            currentOdometer = 47_000,
            today = today,
        )

        assertEquals(MaintenanceStatus.OVERDUE, status.status)
        assertEquals(0, status.milesLeft)
    }

    @Test
    fun aMileageIntervalWithNoBaselineLeavesTheCheckInactive() {
        val status = statusOf(
            item(mileageInterval = 5_000, reminder = Interval(2, IntervalUnit.YEARS)),
            currentOdometer = 48_000,
            today = today,
        )

        assertEquals(MaintenanceStatus.OK, status.status)
        assertNull(status.mileageDue)
        assertNull(status.milesLeft)
    }

    @Test
    fun aBaselineWithNoLoggedReadingLeavesTheCheckInactive() {
        val status = statusOf(
            item(mileageInterval = 5_000, lastDoneMileage = 42_000, reminder = Interval(2, IntervalUnit.YEARS)),
            currentOdometer = null,
            today = today,
        )

        assertEquals(MaintenanceStatus.OK, status.status)
        assertEquals(47_000, status.mileageDue)
        assertNull(status.milesLeft)
    }

    @Test
    fun aClearedLastDoneDateWithNoMileageSignalHasNoStatusAtAll() {
        val status = statusOf(item(lastDoneDate = null), currentOdometer = 48_000, today = today)

        assertEquals(MaintenanceStatus.NONE, status.status)
        assertNull(status.nextReminderDate)
        assertNull(status.dueDate)
        assertNull(status.nextDueDate)
    }

    @Test
    fun aClearedLastDoneDateStillReportsTheMileageCheck() {
        val status = statusOf(
            item(lastDoneDate = null, mileageInterval = 5_000, lastDoneMileage = 42_000),
            currentOdometer = 48_000,
            today = today,
        )

        assertEquals(MaintenanceStatus.OVERDUE, status.status)
    }

    @Test
    fun nextDueDatePrefersTheDueDateAndFallsBackToTheReminder() {
        val recurring = statusOf(
            item(recurrence = Interval(1, IntervalUnit.YEARS)),
            currentOdometer = null,
            today = today,
        )
        val mileageOnly = statusOf(item(), currentOdometer = null, today = today)

        assertEquals(LocalDate.of(2027, 3, 1), recurring.nextDueDate)
        assertEquals(LocalDate.of(2026, 8, 1), mileageOnly.nextDueDate)
    }

    @Test
    fun addingMonthsClampsToTheEndOfAShortMonth() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 1, 31).plusInterval(Interval(1, IntervalUnit.MONTHS)),
        )
    }

    @Test
    fun everyIntervalUnitAdvancesTheDate() {
        val start = LocalDate.of(2026, 1, 1)

        assertEquals(LocalDate.of(2026, 1, 11), start.plusInterval(Interval(10, IntervalUnit.DAYS)))
        assertEquals(LocalDate.of(2026, 1, 15), start.plusInterval(Interval(2, IntervalUnit.WEEKS)))
        assertEquals(LocalDate.of(2026, 7, 1), start.plusInterval(Interval(6, IntervalUnit.MONTHS)))
        assertEquals(LocalDate.of(2028, 1, 1), start.plusInterval(Interval(2, IntervalUnit.YEARS)))
    }

    @Test
    fun anEmptyLogHasNoCurrentOdometer() {
        assertNull(currentOdometer(emptyList()))
    }

    @Test
    fun aBackDatedEntryCannotLowerTheCurrentOdometer() {
        val entries = listOf(
            entry("s-1", LocalDate.of(2026, 3, 1), 48_000),
            entry("s-2", LocalDate.of(2025, 6, 1), 31_000),
        )

        assertEquals(48_000, currentOdometer(entries))
    }

    private fun entry(id: String, date: LocalDate, odometer: Int) = ServiceLogEntry(
        id = id,
        vehicleId = "v-1",
        description = "Oil and filter",
        date = date,
        odometer = odometer,
    )
}

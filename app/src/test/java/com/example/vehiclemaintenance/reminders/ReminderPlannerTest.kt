package com.example.vehiclemaintenance.reminders

import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.vehicles.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ReminderPlannerTest {

    private val zone: ZoneId = ZoneId.of("America/Denver")
    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")
    private val today = LocalDate.of(2026, 9, 6)

    /** Six months of reminder from this date lands exactly on [today]. */
    private val dueToday = LocalDate.of(2026, 3, 6)

    private fun item(
        id: String,
        name: String = "Oil change",
        lastDoneDate: LocalDate? = dueToday,
        lastNotifiedAt: String? = null,
    ) = MaintenanceItem(
        id = id,
        vehicleId = "v-1",
        name = name,
        reminder = Interval(6, IntervalUnit.MONTHS),
        lastDoneDate = lastDoneDate,
        lastNotifiedAt = lastNotifiedAt,
    )

    private fun plan(vararg items: MaintenanceItem) =
        planReminders(listOf(vehicle), mapOf("v-1" to items.toList()), today, zone)

    /** An instant that reads back as [days] before [today] in [zone]. */
    private fun stamp(days: Long): String =
        today.minusDays(days).atTime(18, 30).atZone(zone).toInstant().toString()

    @Test
    fun `an item before its reminder date is not notified and its vehicle is cancelled`() {
        val result = plan(item("m-1", lastDoneDate = dueToday.plusDays(1)))

        assertEquals(emptyList<VehicleReminder>(), result.notify)
        assertEquals(listOf("v-1"), result.cancelVehicleIds)
        assertEquals(emptySet<String>(), result.stampItemIds)
    }

    @Test
    fun `a first time due item is notified and stamped`() {
        val result = plan(item("m-1"))

        assertEquals(listOf(VehicleReminder(vehicle, listOf("Oil change"))), result.notify)
        assertEquals(emptyList<String>(), result.cancelVehicleIds)
        assertEquals(setOf("m-1"), result.stampItemIds)
    }

    @Test
    fun `an item notified less than two weeks ago posts nothing and cancels nothing`() {
        val result = plan(item("m-1", lastNotifiedAt = stamp(13)))

        assertEquals(emptyList<VehicleReminder>(), result.notify)
        assertEquals(emptyList<String>(), result.cancelVehicleIds)
        assertEquals(emptySet<String>(), result.stampItemIds)
    }

    @Test
    fun `an item notified exactly two weeks ago is notified again`() {
        val result = plan(item("m-1", lastNotifiedAt = stamp(REMINDER_REPEAT_DAYS)))

        assertEquals(listOf(VehicleReminder(vehicle, listOf("Oil change"))), result.notify)
        assertEquals(setOf("m-1"), result.stampItemIds)
    }

    @Test
    fun `a newly due item lists and stamps every due item on the vehicle`() {
        val suppressed = item("m-1", name = "Oil change", lastNotifiedAt = stamp(3))
        val newlyDue = item("m-2", name = "Tire rotation")

        val result = plan(suppressed, newlyDue)

        assertEquals(
            listOf(VehicleReminder(vehicle, listOf("Oil change", "Tire rotation"))),
            result.notify,
        )
        assertEquals(setOf("m-1", "m-2"), result.stampItemIds)
    }

    @Test
    fun `an item with no last done date is never notified`() {
        val result = plan(item("m-1", lastDoneDate = null))

        assertEquals(emptyList<VehicleReminder>(), result.notify)
        assertEquals(listOf("v-1"), result.cancelVehicleIds)
    }

    @Test
    fun `an unparsable last notified stamp is treated as never notified`() {
        val result = plan(item("m-1", lastNotifiedAt = "yesterday"))

        assertEquals(listOf(VehicleReminder(vehicle, listOf("Oil change"))), result.notify)
        assertEquals(setOf("m-1"), result.stampItemIds)
    }

    @Test
    fun `a vehicle with no items at all is cancelled`() {
        val result = planReminders(listOf(vehicle), emptyMap(), today, zone)

        assertEquals(listOf("v-1"), result.cancelVehicleIds)
    }

    @Test
    fun `vehicles are planned independently of each other`() {
        val other = Vehicle("v-2", 2020, "Honda", "Civic", "2.0L")
        val result = planReminders(
            listOf(vehicle, other),
            mapOf(
                "v-1" to listOf(item("m-1")),
                "v-2" to listOf(item("m-2").copy(vehicleId = "v-2", lastDoneDate = today)),
            ),
            today,
            zone,
        )

        assertEquals(listOf(VehicleReminder(vehicle, listOf("Oil change"))), result.notify)
        assertEquals(listOf("v-2"), result.cancelVehicleIds)
        assertEquals(setOf("m-1"), result.stampItemIds)
    }
}

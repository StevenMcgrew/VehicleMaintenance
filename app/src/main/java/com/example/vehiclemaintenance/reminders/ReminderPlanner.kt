package com.example.vehiclemaintenance.reminders

import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.maintenance.plusInterval
import com.example.vehiclemaintenance.vehicles.Vehicle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** Days between the first reminder and each repeat, until the service is logged. */
const val REMINDER_REPEAT_DAYS = 14L

/** One vehicle's notification: every item of its that is past its reminder date. */
data class VehicleReminder(
    val vehicle: Vehicle,
    val dueItemNames: List<String>,
)

data class ReminderPlan(
    val notify: List<VehicleReminder> = emptyList(),
    val cancelVehicleIds: List<String> = emptyList(),
    val stampItemIds: Set<String> = emptySet(),
)

/**
 * Decides what the daily check should post, cancel, and stamp. Pure so every cadence case is
 * testable without waiting: [today] and [zone] are supplied rather than read from the system.
 */
fun planReminders(
    vehicles: List<Vehicle>,
    itemsByVehicle: Map<String, List<MaintenanceItem>>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): ReminderPlan {
    val notify = mutableListOf<VehicleReminder>()
    val cancelVehicleIds = mutableListOf<String>()
    val stampItemIds = mutableSetOf<String>()

    for (vehicle in vehicles) {
        val due = itemsByVehicle[vehicle.id].orEmpty().filter { it.isDueForNotification(today) }
        if (due.isEmpty()) {
            cancelVehicleIds += vehicle.id
            continue
        }
        // Nothing has come round again yet, so leave any notification the user has not read alone.
        if (due.none { it.isReadyToNotify(today, zone) }) continue

        notify += VehicleReminder(vehicle, due.map { it.name })
        // The notification names every due item, so every due item's cadence restarts together.
        // Stamping only the ones that came due would drift into a notification every few days.
        stampItemIds += due.map { it.id }
    }
    return ReminderPlan(notify, cancelVehicleIds, stampItemIds)
}

/** Only the reminder clock notifies. Recurrence and mileage drive the on-screen status instead. */
private fun MaintenanceItem.isDueForNotification(today: LocalDate): Boolean {
    val reminderDate = lastDoneDate?.plusInterval(reminder) ?: return false
    return !today.isBefore(reminderDate)
}

private fun MaintenanceItem.isReadyToNotify(today: LocalDate, zone: ZoneId): Boolean {
    val notified = lastNotifiedAt?.let(::parseStamp) ?: return true
    return ChronoUnit.DAYS.between(notified.atZone(zone).toLocalDate(), today) >= REMINDER_REPEAT_DAYS
}

/** A stamp we cannot read means never notified, so the reminder fires and the field is rewritten. */
private fun parseStamp(value: String): Instant? = try {
    Instant.parse(value)
} catch (e: DateTimeParseException) {
    null
}

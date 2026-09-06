package com.example.vehiclemaintenance.maintenance

import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import java.time.LocalDate

enum class MaintenanceStatus { OK, DUE, OVERDUE, NONE }

/**
 * Everything the detail screen shows about one item, all derived at read time. None of it is
 * persisted, so a change to an interval is reflected the moment the store emits.
 */
data class MaintenanceItemStatus(
    val status: MaintenanceStatus,
    val nextReminderDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val mileageDue: Int? = null,
    val milesLeft: Int? = null,
) {
    /** The date the user acts on: the service due date, or the reminder when nothing recurs. */
    val nextDueDate: LocalDate? get() = dueDate ?: nextReminderDate

    val isOverdueByMileage: Boolean get() = milesLeft != null && milesLeft <= 0
}

/** Clamps to the end of a short month, so 31 January plus one month is 28 or 29 February. */
fun LocalDate.plusInterval(interval: Interval): LocalDate {
    val amount = interval.value.toLong()
    return when (interval.unit) {
        IntervalUnit.DAYS -> plusDays(amount)
        IntervalUnit.WEEKS -> plusWeeks(amount)
        IntervalUnit.MONTHS -> plusMonths(amount)
        IntervalUnit.YEARS -> plusYears(amount)
    }
}

/**
 * The highest reading, not the newest entry. A repair logged today can be back dated to work the
 * user forgot, and that lower reading must never walk the vehicle's known mileage backwards.
 * Null until something is logged, which is what keeps the mileage check inactive until then.
 */
fun currentOdometer(entries: List<ServiceLogEntry>): Int? = entries.maxOfOrNull { it.odometer }

/**
 * Both thresholds are inclusive: an item reaches its due point on the exact day and the exact
 * mile, not the day after.
 */
fun statusOf(
    item: MaintenanceItem,
    currentOdometer: Int?,
    today: LocalDate,
): MaintenanceItemStatus {
    val nextReminderDate = item.lastDoneDate?.plusInterval(item.reminder)
    val dueDate = item.lastDoneDate?.let { done -> item.recurrence?.let { done.plusInterval(it) } }
    val mileageDue = item.lastDoneMileage?.let { done -> item.mileageInterval?.let { done + it } }
    val milesLeft = if (mileageDue != null && currentOdometer != null) {
        mileageDue - currentOdometer
    } else {
        null
    }

    val derived = MaintenanceItemStatus(
        status = MaintenanceStatus.OK,
        nextReminderDate = nextReminderDate,
        dueDate = dueDate,
        mileageDue = mileageDue,
        milesLeft = milesLeft,
    )
    val status = when {
        dueDate != null && !today.isBefore(dueDate) -> MaintenanceStatus.OVERDUE
        derived.isOverdueByMileage -> MaintenanceStatus.OVERDUE
        nextReminderDate != null && !today.isBefore(nextReminderDate) -> MaintenanceStatus.DUE
        // Nothing was computable at all: no last done date, and no usable mileage baseline.
        nextReminderDate == null && dueDate == null && milesLeft == null -> MaintenanceStatus.NONE
        else -> MaintenanceStatus.OK
    }
    return derived.copy(status = status)
}

package com.example.vehiclemaintenance.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.StoreResult
import java.time.Instant
import java.time.LocalDate

/** The daily check: what is past its reminder date, and has it been two weeks since we last said so. */
class DueReminderWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as VehicleMaintenanceApplication).container
        val vehicles = container.vehicleRepository
        val items = container.maintenanceItemRepository

        // This can be the first read in a fresh process, and a store we could not parse is never
        // something to post reminders about.
        if (vehicles.load() is StoreResult.Failure) return Result.retry()

        val all = vehicles.vehicles.value
        val plan = planReminders(
            vehicles = all,
            itemsByVehicle = all.associate { it.id to items.itemsFor(it.id).value },
            today = LocalDate.now(),
        )

        val notifier = ReminderNotifier(applicationContext)
        plan.cancelVehicleIds.forEach(notifier::cancel)
        plan.notify.forEach(notifier::post)

        if (plan.stampItemIds.isEmpty()) return Result.success()
        // Posting before stamping costs at most one duplicate on a crash, which beats a silent miss.
        return when (items.markNotified(plan.stampItemIds, Instant.now())) {
            is StoreResult.Success -> Result.success()
            is StoreResult.Failure -> Result.retry()
        }
    }
}

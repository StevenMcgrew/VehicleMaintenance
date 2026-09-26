package com.example.vehiclemaintenance.servicelog

import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.StoreUpdate
import com.example.vehiclemaintenance.data.mapState
import com.example.vehiclemaintenance.maintenance.currentOdometer
import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.util.UUID

/** The fields the user supplies; the repository owns the id and the clock reset. */
data class ServiceLogDraft(
    val vehicleId: String,
    val maintenanceItemId: String? = null,
    val description: String,
    val date: LocalDate,
    val odometer: Int,
    val cost: Int? = null,
    val notes: String? = null,
)

interface ServiceLogRepository {
    /** Every vehicle's entries in stored order. */
    val allEntries: StateFlow<List<ServiceLogEntry>>

    /** The vehicle's history, newest first. */
    fun entriesFor(vehicleId: String): StateFlow<List<ServiceLogEntry>>

    suspend fun add(draft: ServiceLogDraft): StoreResult<ServiceLogEntry>
}

class JsonServiceLogRepository(
    private val holder: MaintenanceStoreHolder,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ServiceLogRepository {

    override val allEntries: StateFlow<List<ServiceLogEntry>> =
        holder.state.mapState { it.serviceLogEntries }

    /**
     * Reversing before the stable sort is what orders entries that share a date: new entries are
     * appended, so the reversal puts the most recently logged one first and equal dates keep it.
     */
    override fun entriesFor(vehicleId: String): StateFlow<List<ServiceLogEntry>> =
        holder.state.mapState { store ->
            store.serviceLogEntries
                .filter { it.vehicleId == vehicleId }
                .asReversed()
                .sortedByDescending { it.date }
        }

    /**
     * Appends the entry and resets its item's clocks in one write, so a failure can never log the
     * service and lose the reset, which would keep telling the user an item is overdue.
     */
    override suspend fun add(draft: ServiceLogDraft): StoreResult<ServiceLogEntry> =
        holder.update { store ->
            val itemId = draft.maintenanceItemId
            if (itemId != null && store.maintenanceItems.none { it.id == itemId }) {
                return@update StoreUpdate.Reject(
                    IllegalArgumentException("No maintenance item with id $itemId"),
                )
            }
            val entry = ServiceLogEntry(
                id = newId(),
                vehicleId = draft.vehicleId,
                maintenanceItemId = itemId,
                description = draft.description,
                date = draft.date,
                odometer = draft.odometer,
                cost = draft.cost,
                notes = draft.notes,
            )
            val items = if (itemId == null) {
                store.maintenanceItems
            } else {
                store.maintenanceItems.map { item ->
                    if (item.id != itemId) {
                        item
                    } else {
                        item.copy(
                            lastDoneDate = draft.date,
                            lastDoneMileage = draft.odometer,
                            // Logging the service cancels any repeat feature 7 has scheduled.
                            lastNotifiedAt = null,
                        )
                    }
                }
            }
            StoreUpdate.Write(
                store.copy(
                    vehicles = store.vehicles.raiseMileage(entry, store.serviceLogEntries),
                    maintenanceItems = items,
                    serviceLogEntries = store.serviceLogEntries + entry,
                ),
                entry,
            )
        }
}

/** Only a higher reading moves the vehicle forward, so a back dated entry never lowers it. */
private fun List<Vehicle>.raiseMileage(
    entry: ServiceLogEntry,
    existing: List<ServiceLogEntry>,
): List<Vehicle> = map { vehicle ->
    if (vehicle.id != entry.vehicleId) return@map vehicle
    val current = currentOdometer(
        vehicle.recordedMileage,
        existing.filter { it.vehicleId == vehicle.id },
    )
    if (current == null || entry.odometer > current) {
        vehicle.copy(recordedMileage = entry.odometer)
    } else {
        vehicle
    }
}

package com.example.vehiclemaintenance.maintenance

import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.StoreUpdate
import com.example.vehiclemaintenance.data.mapState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.LocalDate
import java.util.UUID

private const val MAINTENANCE_ITEM_ID = "maintenanceItemId"

/** The fields the user supplies; the repository owns the id and the last-done baseline. */
data class MaintenanceItemDraft(
    val vehicleId: String,
    val name: String,
    val mileageInterval: Int? = null,
    val recurrence: Interval? = null,
    val reminder: Interval,
    val lastDoneDate: LocalDate? = null,
    val lastDoneMileage: Int? = null,
)

interface MaintenanceItemRepository {
    fun itemsFor(vehicleId: String): StateFlow<List<MaintenanceItem>>

    suspend fun add(draft: MaintenanceItemDraft): StoreResult<MaintenanceItem>

    suspend fun update(item: MaintenanceItem): StoreResult<Unit>

    suspend fun delete(itemId: String): StoreResult<Unit>
}

class JsonMaintenanceItemRepository(
    private val holder: MaintenanceStoreHolder,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val today: () -> LocalDate = { LocalDate.now() },
) : MaintenanceItemRepository {

    override fun itemsFor(vehicleId: String): StateFlow<List<MaintenanceItem>> =
        holder.state.mapState { store -> store.maintenanceItems.filter { it.vehicleId == vehicleId } }

    override suspend fun add(draft: MaintenanceItemDraft): StoreResult<MaintenanceItem> =
        holder.update { store ->
            val item = MaintenanceItem(
                id = newId(),
                vehicleId = draft.vehicleId,
                name = draft.name,
                mileageInterval = draft.mileageInterval,
                recurrence = draft.recurrence,
                reminder = draft.reminder,
                // Skipping the seeding starts the reminder clock at creation instead.
                lastDoneDate = draft.lastDoneDate ?: today(),
                lastDoneMileage = draft.lastDoneMileage,
            )
            StoreUpdate.Write(store.copy(maintenanceItems = store.maintenanceItems + item), item)
        }

    override suspend fun update(item: MaintenanceItem): StoreResult<Unit> = holder.update { store ->
        if (store.maintenanceItems.none { it.id == item.id }) {
            StoreUpdate.Reject(IllegalArgumentException("No maintenance item with id ${item.id}"))
        } else {
            val updated = store.maintenanceItems.map { if (it.id == item.id) item else it }
            StoreUpdate.Write(store.copy(maintenanceItems = updated), Unit)
        }
    }

    /**
     * Removes the item but never the work logged against it. Entries keep their own description, so
     * clearing the link preserves the history as an ad-hoc repair rather than leaving a dangling id.
     */
    override suspend fun delete(itemId: String): StoreResult<Unit> = holder.update { store ->
        StoreUpdate.Write(
            store.copy(
                maintenanceItems = store.maintenanceItems.filterNot { it.id == itemId },
                serviceLogEntries = store.serviceLogEntries.map { it.unlinkFrom(itemId) },
            ),
            Unit,
        )
    }
}

private fun JsonObject.unlinkFrom(itemId: String): JsonObject =
    if ((this[MAINTENANCE_ITEM_ID] as? JsonPrimitive)?.content == itemId) {
        JsonObject(this - MAINTENANCE_ITEM_ID)
    } else {
        this
    }

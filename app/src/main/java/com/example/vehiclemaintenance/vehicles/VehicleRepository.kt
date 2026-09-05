package com.example.vehiclemaintenance.vehicles

import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.StoreUpdate
import com.example.vehiclemaintenance.data.mapState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/** The fields the user supplies; the repository owns id assignment. */
data class VehicleDraft(
    val year: Int,
    val make: String,
    val model: String,
    val engine: String,
)

interface VehicleRepository {
    val vehicles: StateFlow<List<Vehicle>>

    suspend fun load(): StoreResult<Unit>

    suspend fun add(draft: VehicleDraft): StoreResult<Vehicle>

    suspend fun update(vehicle: Vehicle): StoreResult<Unit>

    suspend fun delete(vehicleId: String): StoreResult<Unit>
}

class JsonVehicleRepository(
    private val holder: MaintenanceStoreHolder,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : VehicleRepository {

    override val vehicles: StateFlow<List<Vehicle>> = holder.state.mapState { it.vehicles }

    override suspend fun load(): StoreResult<Unit> = holder.load()

    override suspend fun add(draft: VehicleDraft): StoreResult<Vehicle> = holder.update { store ->
        val vehicle = Vehicle(
            id = newId(),
            year = draft.year,
            make = draft.make,
            model = draft.model,
            engine = draft.engine,
        )
        StoreUpdate.Write(store.copy(vehicles = store.vehicles + vehicle), vehicle)
    }

    override suspend fun update(vehicle: Vehicle): StoreResult<Unit> = holder.update { store ->
        if (store.vehicles.none { it.id == vehicle.id }) {
            StoreUpdate.Reject(IllegalArgumentException("No vehicle with id ${vehicle.id}"))
        } else {
            val updated = store.vehicles.map { if (it.id == vehicle.id) vehicle else it }
            StoreUpdate.Write(store.copy(vehicles = updated), Unit)
        }
    }

    override suspend fun delete(vehicleId: String): StoreResult<Unit> = holder.update { store ->
        StoreUpdate.Write(
            store.copy(
                vehicles = store.vehicles.filterNot { it.id == vehicleId },
                maintenanceItems = store.maintenanceItems.filterNot { it.vehicleId == vehicleId },
                serviceLogEntries = store.serviceLogEntries.filterNot { it.belongsTo(vehicleId) },
            ),
            Unit,
        )
    }
}

private fun JsonObject.belongsTo(vehicleId: String): Boolean =
    (this["vehicleId"] as? JsonPrimitive)?.content == vehicleId

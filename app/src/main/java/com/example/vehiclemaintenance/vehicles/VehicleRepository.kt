package com.example.vehiclemaintenance.vehicles

import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.StoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/** The fields the user supplies; the repository owns id assignment. */
data class VehicleDraft(
    val nickname: String?,
    val year: Int,
    val make: String,
    val model: String,
    val engine: String,
)

/** Raised when the store could not be read, so writing would overwrite data we failed to parse. */
class StoreUnavailableException : IllegalStateException(
    "The maintenance store could not be read, so changes cannot be saved.",
)

interface VehicleRepository {
    val vehicles: StateFlow<List<Vehicle>>

    suspend fun load(): StoreResult<Unit>

    suspend fun add(draft: VehicleDraft): StoreResult<Vehicle>

    suspend fun update(vehicle: Vehicle): StoreResult<Unit>

    suspend fun delete(vehicleId: String): StoreResult<Unit>
}

class JsonVehicleRepository(
    private val store: JsonFileStore,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : VehicleRepository {

    private val mutex = Mutex()
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    override val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private var cached: MaintenanceStore? = null

    override suspend fun load(): StoreResult<Unit> = mutex.withLock {
        when (val result = store.load()) {
            is StoreResult.Success -> {
                cached = result.value
                _vehicles.value = result.value.vehicles
                StoreResult.Success(Unit)
            }

            is StoreResult.Failure -> {
                cached = null
                _vehicles.value = emptyList()
                result
            }
        }
    }

    override suspend fun add(draft: VehicleDraft): StoreResult<Vehicle> = mutex.withLock {
        val current = cached ?: return@withLock StoreResult.Failure(StoreUnavailableException())
        val vehicle = Vehicle(
            id = newId(),
            nickname = draft.nickname,
            year = draft.year,
            make = draft.make,
            model = draft.model,
            engine = draft.engine,
        )
        when (val saved = persist(current.copy(vehicles = current.vehicles + vehicle))) {
            is StoreResult.Success -> StoreResult.Success(vehicle)
            is StoreResult.Failure -> saved
        }
    }

    override suspend fun update(vehicle: Vehicle): StoreResult<Unit> = mutex.withLock {
        val current = cached ?: return@withLock StoreResult.Failure(StoreUnavailableException())
        if (current.vehicles.none { it.id == vehicle.id }) {
            return@withLock StoreResult.Failure(
                IllegalArgumentException("No vehicle with id ${vehicle.id}"),
            )
        }
        val updated = current.vehicles.map { if (it.id == vehicle.id) vehicle else it }
        persist(current.copy(vehicles = updated))
    }

    override suspend fun delete(vehicleId: String): StoreResult<Unit> = mutex.withLock {
        val current = cached ?: return@withLock StoreResult.Failure(StoreUnavailableException())
        persist(
            current.copy(
                vehicles = current.vehicles.filterNot { it.id == vehicleId },
                maintenanceItems = current.maintenanceItems.filterNot { it.belongsTo(vehicleId) },
                serviceLogEntries = current.serviceLogEntries.filterNot { it.belongsTo(vehicleId) },
            ),
        )
    }

    private suspend fun persist(next: MaintenanceStore): StoreResult<Unit> =
        when (val result = store.save(next)) {
            is StoreResult.Success -> {
                cached = next
                _vehicles.value = next.vehicles
                StoreResult.Success(Unit)
            }

            is StoreResult.Failure -> result
        }
}

private fun JsonObject.belongsTo(vehicleId: String): Boolean =
    (this["vehicleId"] as? JsonPrimitive)?.content == vehicleId

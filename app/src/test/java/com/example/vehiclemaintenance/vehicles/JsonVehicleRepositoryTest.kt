package com.example.vehiclemaintenance.vehicles

import com.example.vehiclemaintenance.data.CURRENT_SCHEMA_VERSION
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.StoreUnavailableException
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class JsonVehicleRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    private val draft = VehicleDraft(
        year = 2014,
        make = "Toyota",
        model = "Tacoma",
        engine = "4.0L V6",
    )

    @Before
    fun setUp() {
        file = File(folder.root, "vehicle-maintenance.json")
    }

    private fun holder() = MaintenanceStoreHolder(JsonFileStore(file))

    private fun repository(vararg ids: String): JsonVehicleRepository =
        repositoryOver(holder(), *ids)

    private fun repositoryOver(
        holder: MaintenanceStoreHolder,
        vararg ids: String,
    ): JsonVehicleRepository {
        val queue = ids.toMutableList()
        return JsonVehicleRepository(holder) { queue.removeAt(0) }
    }

    private fun entry(id: String, vehicleId: String) = ServiceLogEntry(
        id = id,
        vehicleId = vehicleId,
        description = "Oil and filter",
        date = LocalDate.of(2026, 3, 15),
        odometer = 42000,
    )

    private fun item(id: String, vehicleId: String) = MaintenanceItem(
        id = id,
        vehicleId = vehicleId,
        name = "Oil change",
        reminder = Interval(5, IntervalUnit.MONTHS),
    )

    private fun seed(store: MaintenanceStore) {
        file.writeText(storeJson.encodeToString(store))
    }

    @Test
    fun `add appends a vehicle carrying the injected id`() = runBlocking {
        val repository = repository("id-1")
        repository.load()

        val added = repository.add(draft)

        val vehicle = (added as StoreResult.Success).value
        assertEquals("id-1", vehicle.id)
        assertEquals(draft.model, vehicle.model)
        assertEquals(listOf(vehicle), repository.vehicles.value)
    }

    @Test
    fun `add leaves existing entries untouched`() = runBlocking {
        seed(
            MaintenanceStore(
                maintenanceItems = listOf(item("m-1", "other")),
                serviceLogEntries = listOf(entry("s-1", "other")),
            ),
        )
        val repository = repository("id-1")
        repository.load()

        repository.add(draft)

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(file.readText())
        assertEquals(listOf(item("m-1", "other")), onDisk.maintenanceItems)
        assertEquals(listOf(entry("s-1", "other")), onDisk.serviceLogEntries)
        assertEquals(CURRENT_SCHEMA_VERSION, onDisk.schemaVersion)
    }

    @Test
    fun `update preserves the id and the list position`() = runBlocking {
        val repository = repository("id-1", "id-2")
        repository.load()
        repository.add(draft)
        val second = (repository.add(draft.copy(model = "Second")) as StoreResult.Success).value

        val result = repository.update(second.copy(model = "Renamed", engine = "2.7L I4"))

        assertTrue(result is StoreResult.Success)
        val vehicles = repository.vehicles.value
        assertEquals(listOf("id-1", "id-2"), vehicles.map { it.id })
        assertEquals("Renamed", vehicles[1].model)
        assertEquals("2.7L I4", vehicles[1].engine)
    }

    @Test
    fun `delete cascades to the deleted vehicle's items and log entries only`() = runBlocking {
        seed(
            MaintenanceStore(
                vehicles = listOf(
                    Vehicle("keep", 2020, "Honda", "Civic", "2.0L"),
                    Vehicle("drop", 2014, "Toyota", "Tacoma", "4.0L V6"),
                ),
                maintenanceItems = listOf(item("m-keep", "keep"), item("m-drop", "drop")),
                serviceLogEntries = listOf(entry("s-keep", "keep"), entry("s-drop", "drop")),
            ),
        )
        val repository = repository()
        repository.load()

        val result = repository.delete("drop")

        assertTrue(result is StoreResult.Success)
        val onDisk = storeJson.decodeFromString<MaintenanceStore>(file.readText())
        assertEquals(listOf("keep"), onDisk.vehicles.map { it.id })
        assertEquals(listOf(item("m-keep", "keep")), onDisk.maintenanceItems)
        assertEquals(listOf(entry("s-keep", "keep")), onDisk.serviceLogEntries)
    }

    @Test
    fun `mutations are visible to a fresh repository over the same file`() = runBlocking {
        val repository = repository("id-1")
        repository.load()
        repository.add(draft)

        val reopened = repository()
        reopened.load()

        assertEquals("id-1", reopened.vehicles.value.single().id)
        assertEquals("Tacoma", reopened.vehicles.value.single().model)
    }

    @Test
    fun `a failed load blocks every write`() = runBlocking {
        file.writeText("{ not json")
        val repository = repository("id-1")

        val loaded = repository.load()

        assertTrue(loaded is StoreResult.Failure)
        assertTrue((repository.add(draft) as StoreResult.Failure).cause is StoreUnavailableException)
        val existing = Vehicle("id-1", 2014, "Toyota", "Tacoma", "4.0L V6")
        assertTrue((repository.update(existing) as StoreResult.Failure).cause is StoreUnavailableException)
        assertTrue((repository.delete("id-1") as StoreResult.Failure).cause is StoreUnavailableException)
        assertEquals("{ not json", file.readText())
    }
}

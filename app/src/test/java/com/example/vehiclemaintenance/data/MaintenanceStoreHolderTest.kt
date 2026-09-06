package com.example.vehiclemaintenance.data

import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.Vehicle
import com.example.vehiclemaintenance.vehicles.VehicleDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class MaintenanceStoreHolderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(folder.root, "vehicle-maintenance.json")
    }

    private fun holder() = MaintenanceStoreHolder(JsonFileStore(file))

    private fun draft(make: String) = VehicleDraft(2014, make, "Tacoma", "4.0L V6")

    private fun vehicle(id: String) = Vehicle(id, 2014, "Toyota", "Tacoma", "4.0L V6")

    /** A directory where the store file belongs makes the atomic rename fail. */
    private fun blockWrites() {
        file.delete()
        file.mkdirs()
    }

    private fun entry(id: String, vehicleId: String) = ServiceLogEntry(
        id = id,
        vehicleId = vehicleId,
        description = "Oil and filter",
        date = LocalDate.of(2026, 3, 15),
        odometer = 42000,
    )

    @Test
    fun `a write through one repository is visible to another sharing the holder`() = runBlocking {
        val holder = holder()
        val first = JsonVehicleRepository(holder) { "id-1" }
        val second = JsonVehicleRepository(holder) { "id-2" }
        holder.load()

        first.add(draft("Toyota"))

        assertEquals(listOf("id-1"), second.vehicles.value.map { it.id })
    }

    @Test
    fun `a write through one repository does not drop the other's earlier write`() = runBlocking {
        val holder = holder()
        val first = JsonVehicleRepository(holder) { "id-1" }
        val second = JsonVehicleRepository(holder) { "id-2" }
        holder.load()

        first.add(draft("Toyota"))
        second.add(draft("Honda"))

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(file.readText())
        assertEquals(listOf("id-1", "id-2"), onDisk.vehicles.map { it.id })
        assertEquals(listOf("id-1", "id-2"), first.vehicles.value.map { it.id })
    }

    @Test
    fun `an update through a second repository preserves the first's vehicle`() = runBlocking {
        val holder = holder()
        val first = JsonVehicleRepository(holder) { "id-1" }
        val second = JsonVehicleRepository(holder) { "id-2" }
        holder.load()
        first.add(draft("Toyota"))
        val honda = (second.add(draft("Honda")) as StoreResult.Success).value

        second.update(honda.copy(model = "Civic"))

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(file.readText())
        assertEquals(listOf("Toyota", "Honda"), onDisk.vehicles.map { it.make })
        assertEquals("Civic", onDisk.vehicles[1].model)
    }

    @Test
    fun `a failed load blocks every write and leaves the file untouched`() = runBlocking {
        file.writeText("{ not json")
        val holder = holder()

        val loaded = holder.load()

        assertTrue(loaded is StoreResult.Failure)
        val write = holder.update { StoreUpdate.Write(MaintenanceStore(), Unit) }
        assertTrue((write as StoreResult.Failure).cause is StoreUnavailableException)
        assertEquals("{ not json", file.readText())
    }

    @Test
    fun `a rejected update leaves the store and the file unchanged`() = runBlocking {
        val holder = holder()
        holder.load()
        val repository = JsonVehicleRepository(holder) { "id-1" }
        repository.add(draft("Toyota"))
        val before = file.readText()

        val result = holder.update<Unit> { StoreUpdate.Reject(IllegalStateException("no")) }

        assertTrue(result is StoreResult.Failure)
        assertEquals(before, file.readText())
        assertEquals(listOf("id-1"), holder.state.value.vehicles.map { it.id })
    }

    @Test
    fun `entries owned by another repository survive a write`() = runBlocking {
        file.writeText(
            storeJson.encodeToString(
                MaintenanceStore(serviceLogEntries = listOf(entry("s-1", "other"))),
            ),
        )
        val holder = holder()
        holder.load()
        val repository = JsonVehicleRepository(holder) { "id-1" }

        repository.add(draft("Toyota"))

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(file.readText())
        assertEquals(listOf(entry("s-1", "other")), onDisk.serviceLogEntries)
    }

    @Test
    fun `replace overwrites an unparseable file and unblocks later writes`() = runBlocking {
        file.writeText("{ not json")
        val holder = holder()
        holder.load()

        val replaced = holder.replace(MaintenanceStore(vehicles = listOf(vehicle("v-1"))))

        assertTrue(replaced is StoreResult.Success)
        assertEquals(listOf("v-1"), holder.state.value.vehicles.map { it.id })
        assertEquals(
            listOf("v-1"),
            storeJson.decodeFromString<MaintenanceStore>(file.readText()).vehicles.map { it.id },
        )
        val write = JsonVehicleRepository(holder) { "id-2" }.add(draft("Honda"))
        assertTrue(write is StoreResult.Success)
    }

    @Test
    fun `a failed replace leaves the loaded store untouched`() = runBlocking {
        val holder = holder()
        holder.load()
        JsonVehicleRepository(holder) { "id-1" }.add(draft("Toyota"))
        blockWrites()

        val result = holder.replace(MaintenanceStore(vehicles = listOf(vehicle("v-9"))))

        assertTrue(result is StoreResult.Failure)
        assertTrue(holder.isLoaded)
        assertEquals(listOf("id-1"), holder.state.value.vehicles.map { it.id })
    }

    @Test
    fun `a failed replace after a failed load keeps writes blocked`() = runBlocking {
        file.writeText("{ not json")
        val holder = holder()
        holder.load()
        blockWrites()

        val result = holder.replace(MaintenanceStore(vehicles = listOf(vehicle("v-9"))))

        assertTrue(result is StoreResult.Failure)
        assertFalse(holder.isLoaded)
        val write = holder.update { StoreUpdate.Write(MaintenanceStore(), Unit) }
        assertTrue((write as StoreResult.Failure).cause is StoreUnavailableException)
    }

    @Test
    fun `state exposes the loaded store to every reader`() = runBlocking {
        val holder = holder()
        holder.load()
        JsonVehicleRepository(holder) { "id-1" }.add(draft("Toyota"))

        assertEquals(
            Vehicle("id-1", 2014, "Toyota", "Tacoma", "4.0L V6"),
            holder.state.value.vehicles.single(),
        )
    }
}

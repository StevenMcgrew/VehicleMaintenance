package com.example.vehiclemaintenance.maintenance

import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class JsonMaintenanceItemRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    private val created = LocalDate.of(2026, 9, 5)

    private val draft = MaintenanceItemDraft(
        vehicleId = "v-1",
        name = "Oil change",
        mileageInterval = 5000,
        recurrence = Interval(6, IntervalUnit.MONTHS),
        reminder = Interval(5, IntervalUnit.MONTHS),
    )

    @Before
    fun setUp() {
        file = File(folder.root, "vehicle-maintenance.json")
    }

    private fun holder() = MaintenanceStoreHolder(JsonFileStore(file))

    private fun repository(
        holder: MaintenanceStoreHolder,
        vararg ids: String,
    ): JsonMaintenanceItemRepository {
        val queue = ids.toMutableList()
        return JsonMaintenanceItemRepository(holder, { queue.removeAt(0) }, { created })
    }

    private fun seed(store: MaintenanceStore) = file.writeText(storeJson.encodeToString(store))

    private fun logEntry(id: String, vehicleId: String, itemId: String?) = ServiceLogEntry(
        id = id,
        vehicleId = vehicleId,
        maintenanceItemId = itemId,
        description = "Oil and filter",
        date = LocalDate.of(2026, 3, 15),
        odometer = 42000,
    )

    private fun onDisk() = storeJson.decodeFromString<MaintenanceStore>(file.readText())

    @Test
    fun `add appends an item carrying the injected id and owning vehicle`() = runBlocking {
        val holder = holder()
        val repository = repository(holder, "m-1")
        holder.load()

        val item = (repository.add(draft) as StoreResult.Success).value

        assertEquals("m-1", item.id)
        assertEquals("v-1", item.vehicleId)
        assertEquals("Oil change", item.name)
        assertEquals(Interval(5, IntervalUnit.MONTHS), item.reminder)
        assertEquals(listOf(item), repository.itemsFor("v-1").value)
    }

    @Test
    fun `skipped seeding backfills the last done date with the creation date`() = runBlocking {
        val holder = holder()
        val repository = repository(holder, "m-1")
        holder.load()

        val item = (repository.add(draft) as StoreResult.Success).value

        assertEquals(created, item.lastDoneDate)
        assertNull(item.lastDoneMileage)
    }

    @Test
    fun `a supplied last done date and mileage are stored as given`() = runBlocking {
        val holder = holder()
        val repository = repository(holder, "m-1")
        holder.load()
        val seeded = draft.copy(lastDoneDate = LocalDate.of(2026, 1, 2), lastDoneMileage = 41000)

        val item = (repository.add(seeded) as StoreResult.Success).value

        assertEquals(LocalDate.of(2026, 1, 2), item.lastDoneDate)
        assertEquals(41000, item.lastDoneMileage)
    }

    @Test
    fun `update replaces fields while preserving the id and list position`() = runBlocking {
        val holder = holder()
        val repository = repository(holder, "m-1", "m-2")
        holder.load()
        repository.add(draft)
        val second = (repository.add(draft.copy(name = "Tire rotation")) as StoreResult.Success).value

        val result = repository.update(second.copy(name = "Tire rotation and balance"))

        assertTrue(result is StoreResult.Success)
        val items = repository.itemsFor("v-1").value
        assertEquals(listOf("m-1", "m-2"), items.map { it.id })
        assertEquals("Tire rotation and balance", items[1].name)
    }

    @Test
    fun `items for a vehicle exclude another vehicle's items`() = runBlocking {
        val holder = holder()
        val repository = repository(holder, "m-1", "m-2")
        holder.load()
        repository.add(draft)
        repository.add(draft.copy(vehicleId = "v-2", name = "Brake fluid"))

        assertEquals(listOf("m-1"), repository.itemsFor("v-1").value.map { it.id })
        assertEquals(listOf("m-2"), repository.itemsFor("v-2").value.map { it.id })
    }

    @Test
    fun `deleting an item keeps logged history and clears the link`() = runBlocking {
        seed(
            MaintenanceStore(
                maintenanceItems = listOf(
                    MaintenanceItem("m-1", "v-1", "Oil change", reminder = Interval(5, IntervalUnit.MONTHS)),
                ),
                serviceLogEntries = listOf(
                    logEntry("s-1", "v-1", "m-1"),
                    logEntry("s-2", "v-1", "m-other"),
                ),
            ),
        )
        val holder = holder()
        val repository = repository(holder)
        holder.load()

        val result = repository.delete("m-1")

        assertTrue(result is StoreResult.Success)
        val store = onDisk()
        assertEquals(emptyList<MaintenanceItem>(), store.maintenanceItems)
        assertEquals(2, store.serviceLogEntries.size)
        assertNull(store.serviceLogEntries[0].maintenanceItemId)
        assertEquals("Oil and filter", store.serviceLogEntries[0].description)
        assertEquals("m-other", store.serviceLogEntries[1].maintenanceItemId)
    }

    @Test
    fun `deleting an item with no logged history removes it entirely`() = runBlocking {
        val holder = holder()
        val repository = repository(holder, "m-1")
        holder.load()
        repository.add(draft)

        repository.delete("m-1")

        val store = onDisk()
        assertEquals(emptyList<MaintenanceItem>(), store.maintenanceItems)
        assertEquals(emptyList<ServiceLogEntry>(), store.serviceLogEntries)
    }

    @Test
    fun `deleting a vehicle still removes that vehicle's items`() = runBlocking {
        val holder = holder()
        val items = repository(holder, "m-1", "m-2")
        val vehicles = JsonVehicleRepository(holder) { "v-1" }
        seed(
            MaintenanceStore(
                vehicles = listOf(
                    Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6"),
                    Vehicle("v-2", 2020, "Honda", "Civic", "2.0L"),
                ),
            ),
        )
        holder.load()
        items.add(draft)
        items.add(draft.copy(vehicleId = "v-2"))

        vehicles.delete("v-1")

        assertEquals(listOf("m-2"), onDisk().maintenanceItems.map { it.id })
    }

    @Test
    fun `mutations are visible to a fresh repository over the same file`() = runBlocking {
        val holder = holder()
        val first = repository(holder, "m-1")
        holder.load()
        first.add(draft)

        val reopened = holder()
        val second = repository(reopened)
        reopened.load()

        assertEquals(listOf("m-1"), second.itemsFor("v-1").value.map { it.id })
        assertEquals("Oil change", second.itemsFor("v-1").value.single().name)
    }

    @Test
    fun `a failed load blocks every write`() = runBlocking {
        file.writeText("{ not json")
        val holder = holder()
        val repository = repository(holder, "m-1")

        assertTrue(holder.load() is StoreResult.Failure)

        assertTrue(repository.add(draft) is StoreResult.Failure)
        assertTrue(repository.delete("m-1") is StoreResult.Failure)
        assertEquals("{ not json", file.readText())
    }
}

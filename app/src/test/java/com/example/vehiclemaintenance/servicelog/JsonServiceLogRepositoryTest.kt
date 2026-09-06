package com.example.vehiclemaintenance.servicelog

import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
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

class JsonServiceLogRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    private val loggedOn = LocalDate.of(2026, 9, 5)

    private val draft = ServiceLogDraft(
        vehicleId = "v-1",
        maintenanceItemId = "m-1",
        description = "Oil and filter",
        date = loggedOn,
        odometer = 48000,
        cost = 6499,
        notes = "shop said belts look fine",
    )

    private val item = MaintenanceItem(
        id = "m-1",
        vehicleId = "v-1",
        name = "Oil change",
        mileageInterval = 5000,
        reminder = Interval(5, IntervalUnit.MONTHS),
        lastDoneDate = LocalDate.of(2026, 3, 15),
        lastDoneMileage = 42000,
        lastNotifiedAt = "2026-08-01T09:00:00Z",
    )

    @Before
    fun setUp() {
        file = File(folder.root, "vehicle-maintenance.json")
    }

    private fun holder() = MaintenanceStoreHolder(JsonFileStore(file))

    private fun repository(holder: MaintenanceStoreHolder, vararg ids: String):
        JsonServiceLogRepository {
        val queue = ids.toMutableList()
        return JsonServiceLogRepository(holder) { queue.removeAt(0) }
    }

    private fun seed(store: MaintenanceStore) = file.writeText(storeJson.encodeToString(store))

    private fun onDisk() = storeJson.decodeFromString<MaintenanceStore>(file.readText())

    private fun entry(
        id: String,
        vehicleId: String = "v-1",
        date: LocalDate = loggedOn,
        maintenanceItemId: String? = "m-1",
        description: String = "Oil and filter",
    ) = ServiceLogEntry(
        id = id,
        vehicleId = vehicleId,
        maintenanceItemId = maintenanceItemId,
        description = description,
        date = date,
        odometer = 48000,
    )

    @Test
    fun `add appends an entry carrying the injected id and every drafted field`() = runBlocking {
        seed(MaintenanceStore(maintenanceItems = listOf(item)))
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()

        val entry = (repository.add(draft) as StoreResult.Success).value

        assertEquals(
            ServiceLogEntry(
                id = "s-1",
                vehicleId = "v-1",
                maintenanceItemId = "m-1",
                description = "Oil and filter",
                date = loggedOn,
                odometer = 48000,
                cost = 6499,
                notes = "shop said belts look fine",
            ),
            entry,
        )
        assertEquals(listOf(entry), onDisk().serviceLogEntries)
    }

    @Test
    fun `the entry and the reset item land together in one write`() = runBlocking {
        seed(MaintenanceStore(maintenanceItems = listOf(item)))
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()

        repository.add(draft)

        val store = onDisk()
        assertEquals(listOf("s-1"), store.serviceLogEntries.map { it.id })
        val reset = store.maintenanceItems.single()
        assertEquals(loggedOn, reset.lastDoneDate)
        assertEquals(48000, reset.lastDoneMileage)
    }

    @Test
    fun `logging a service clears the notification stamp`() = runBlocking {
        seed(MaintenanceStore(maintenanceItems = listOf(item)))
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()

        repository.add(draft)

        assertNull(onDisk().maintenanceItems.single().lastNotifiedAt)
    }

    @Test
    fun `the reset leaves the item's other fields and list position alone`() = runBlocking {
        val other = item.copy(id = "m-2", name = "Tire rotation", lastNotifiedAt = null)
        seed(MaintenanceStore(maintenanceItems = listOf(item, other)))
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()

        repository.add(draft)

        val items = onDisk().maintenanceItems
        assertEquals(listOf("m-1", "m-2"), items.map { it.id })
        assertEquals("Oil change", items[0].name)
        assertEquals(5000, items[0].mileageInterval)
        assertEquals(Interval(5, IntervalUnit.MONTHS), items[0].reminder)
        assertEquals(other, items[1])
    }

    @Test
    fun `a draft naming an unknown item is rejected and writes nothing`() = runBlocking {
        seed(MaintenanceStore(maintenanceItems = listOf(item)))
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()
        val before = file.readText()

        val result = repository.add(draft.copy(maintenanceItemId = "m-missing"))

        assertTrue((result as StoreResult.Failure).cause is IllegalArgumentException)
        assertEquals(before, file.readText())
        assertEquals(listOf(item), holder.state.value.maintenanceItems)
    }

    @Test
    fun `a draft with no item link appends without touching any item`() = runBlocking {
        seed(MaintenanceStore(maintenanceItems = listOf(item)))
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()

        val entry = (
            repository.add(
                draft.copy(maintenanceItemId = null, description = "Fixed a flat", cost = null),
            ) as StoreResult.Success
            ).value

        val store = onDisk()
        assertNull(entry.maintenanceItemId)
        assertEquals(listOf("s-1"), store.serviceLogEntries.map { it.id })
        assertEquals(listOf(item), store.maintenanceItems)
    }

    @Test
    fun `a second entry appends after the first`() = runBlocking {
        seed(MaintenanceStore(maintenanceItems = listOf(item)))
        val holder = holder()
        val repository = repository(holder, "s-1", "s-2")
        holder.load()

        repository.add(draft)
        repository.add(draft.copy(date = LocalDate.of(2026, 9, 6), odometer = 48100))

        val store = onDisk()
        assertEquals(listOf("s-1", "s-2"), store.serviceLogEntries.map { it.id })
        assertEquals(48100, store.maintenanceItems.single().lastDoneMileage)
    }

    @Test
    fun `entriesFor returns only the requested vehicle's entries`() = runBlocking {
        seed(
            MaintenanceStore(
                serviceLogEntries = listOf(
                    entry("s-1", vehicleId = "v-1"),
                    entry("s-2", vehicleId = "v-2"),
                    entry("s-3", vehicleId = "v-1"),
                ),
            ),
        )
        val holder = holder()
        val repository = repository(holder, "s-4")
        holder.load()

        assertEquals(
            listOf("s-1", "s-3"),
            repository.entriesFor("v-1").value.map { it.id }.sorted(),
        )
    }

    @Test
    fun `entriesFor orders distinct dates newest first`() = runBlocking {
        seed(
            MaintenanceStore(
                serviceLogEntries = listOf(
                    entry("s-old", date = LocalDate.of(2026, 1, 4)),
                    entry("s-new", date = LocalDate.of(2026, 9, 5)),
                    entry("s-mid", date = LocalDate.of(2026, 5, 20)),
                ),
            ),
        )
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()

        assertEquals(
            listOf("s-new", "s-mid", "s-old"),
            repository.entriesFor("v-1").value.map { it.id },
        )
    }

    @Test
    fun `entriesFor puts the most recently added first among entries sharing a date`() =
        runBlocking {
            seed(
                MaintenanceStore(
                    serviceLogEntries = listOf(
                        entry("s-first", date = loggedOn),
                        entry("s-second", date = loggedOn),
                        entry("s-third", date = loggedOn),
                    ),
                ),
            )
            val holder = holder()
            val repository = repository(holder, "s-1")
            holder.load()

            assertEquals(
                listOf("s-third", "s-second", "s-first"),
                repository.entriesFor("v-1").value.map { it.id },
            )
        }

    @Test
    fun `entriesFor reflects a new entry as soon as the write succeeds`() = runBlocking {
        seed(MaintenanceStore(maintenanceItems = listOf(item)))
        val holder = holder()
        val repository = repository(holder, "s-1")
        holder.load()
        val history = repository.entriesFor("v-1")
        assertEquals(emptyList<ServiceLogEntry>(), history.value)

        repository.add(draft)

        assertEquals(listOf("s-1"), history.value.map { it.id })
    }

    @Test
    fun `entriesFor still lists an entry whose item link was cleared`() = runBlocking {
        seed(
            MaintenanceStore(
                serviceLogEntries = listOf(
                    entry("s-1", maintenanceItemId = null, description = "Replaced the alternator"),
                ),
            ),
        )
        val holder = holder()
        val repository = repository(holder, "s-2")
        holder.load()

        val listed = repository.entriesFor("v-1").value.single()
        assertNull(listed.maintenanceItemId)
        assertEquals("Replaced the alternator", listed.description)
    }

    @Test
    fun `a failed load blocks the write`() = runBlocking {
        file.writeText("{ not json")
        val holder = holder()
        val repository = repository(holder, "s-1")

        assertTrue(holder.load() is StoreResult.Failure)

        assertTrue(repository.add(draft) is StoreResult.Failure)
        assertEquals("{ not json", file.readText())
    }
}

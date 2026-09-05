package com.example.vehiclemaintenance.data

import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JsonFileStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val vehicle = Vehicle(
        id = "v-1",
        year = 2014,
        make = "Toyota",
        model = "Tacoma",
        engine = "4.0L V6",
    )

    private fun storeFile(): File = File(folder.root, "vehicle-maintenance.json")

    private fun store(file: File = storeFile()) = JsonFileStore(file)

    @Test
    fun `a missing file loads as an empty store and creates nothing`() = runBlocking {
        val file = storeFile()

        val result = store(file).load()

        assertEquals(
            MaintenanceStore(schemaVersion = CURRENT_SCHEMA_VERSION),
            (result as StoreResult.Success).value,
        )
        assertFalse(file.exists())
    }

    @Test
    fun `a saved store loads back unchanged`() = runBlocking {
        val file = storeFile()
        val saved = MaintenanceStore(vehicles = listOf(vehicle))

        assertTrue(store(file).save(saved) is StoreResult.Success)
        val loaded = store(file).load()

        assertEquals(saved, (loaded as StoreResult.Success).value)
    }

    @Test
    fun `a successful save leaves no temp file behind`() = runBlocking {
        val file = storeFile()

        store(file).save(MaintenanceStore(vehicles = listOf(vehicle)))

        assertFalse(File(folder.root, "vehicle-maintenance.json.tmp").exists())
        assertEquals(listOf("vehicle-maintenance.json"), folder.root.list()?.sorted())
    }

    @Test
    fun `an unparseable file fails to load and is left byte for byte unchanged`() = runBlocking {
        val file = storeFile()
        val corrupt = "{\"schemaVersion\":1,\"vehicles\":[".toByteArray()
        file.writeBytes(corrupt)

        val result = store(file).load()

        assertTrue(result is StoreResult.Failure)
        assertArrayEquals(corrupt, file.readBytes())
    }

    @Test
    fun `a stale temp file is never read as the store`() = runBlocking {
        val file = storeFile()
        store(file).save(MaintenanceStore(vehicles = listOf(vehicle)))
        File(folder.root, "vehicle-maintenance.json.tmp")
            .writeText("{\"schemaVersion\":1,\"vehicles\":[],\"maintenanceItems\":[],\"serviceLogEntries\":[]}")

        val result = store(file).load()

        assertEquals(listOf(vehicle), (result as StoreResult.Success).value.vehicles)
    }

    @Test
    fun `a save that cannot write leaves the previous file intact`() = runBlocking {
        val file = storeFile()
        val first = MaintenanceStore(vehicles = listOf(vehicle))
        store(file).save(first)
        val before = file.readBytes()
        // A directory at the temp path makes the write fail without touching the target.
        File(folder.root, "vehicle-maintenance.json.tmp").mkdir()

        val result = store(file).save(MaintenanceStore(vehicles = emptyList()))

        assertTrue(result is StoreResult.Failure)
        assertArrayEquals(before, file.readBytes())
        assertEquals(first, (store(file).load() as StoreResult.Success).value)
    }
}

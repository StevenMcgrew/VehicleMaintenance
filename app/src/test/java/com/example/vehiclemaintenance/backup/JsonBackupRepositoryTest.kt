package com.example.vehiclemaintenance.backup

import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.StoreUnavailableException
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class JsonBackupRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(folder.root, "vehicle-maintenance.json")
    }

    private fun holder() = MaintenanceStoreHolder(JsonFileStore(file))

    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")

    @Test
    fun `export refuses before the store has been read`() = runBlocking {
        val result = JsonBackupRepository(holder()).exportSnapshot()

        assertTrue((result as StoreResult.Failure).cause is StoreUnavailableException)
    }

    @Test
    fun `export refuses when the file could not be parsed`() = runBlocking {
        file.writeText("{ not json")
        val holder = holder()
        holder.load()

        val result = JsonBackupRepository(holder).exportSnapshot()

        assertTrue((result as StoreResult.Failure).cause is StoreUnavailableException)
    }

    @Test
    fun `an exported snapshot parses back as the same store`() = runBlocking {
        val stored = MaintenanceStore(vehicles = listOf(vehicle))
        file.writeText(storeJson.encodeToString(stored))
        val holder = holder()
        holder.load()

        val result = JsonBackupRepository(holder).exportSnapshot()

        val json = (result as StoreResult.Success).value
        assertEquals(BackupParse.Valid(stored), parseBackup(json))
    }

    @Test
    fun `applying a backup replaces the file and the in-memory store`() = runBlocking {
        file.writeText(storeJson.encodeToString(MaintenanceStore(vehicles = listOf(vehicle))))
        val holder = holder()
        holder.load()
        val restored = MaintenanceStore(
            vehicles = listOf(Vehicle("v-2", 2020, "Honda", "Civic", "2.0L I4")),
        )

        val result = JsonBackupRepository(holder).applyBackup(restored)

        assertTrue(result is StoreResult.Success)
        assertEquals(restored, holder.state.value)
        assertEquals(restored, storeJson.decodeFromString<MaintenanceStore>(file.readText()))
    }
}

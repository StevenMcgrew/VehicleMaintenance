package com.example.vehiclemaintenance.backup

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.StoreResult
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Export and import over a real ContentResolver stream, which is the part of the picker flow a test
 * can drive. The picker itself stays manual.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val files = BackupFiles(context.contentResolver)

    private lateinit var storeFile: File
    private lateinit var exported: File

    private val store = MaintenanceStore(
        vehicles = listOf(Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")),
        maintenanceItems = listOf(
            MaintenanceItem(
                id = "i-1",
                vehicleId = "v-1",
                name = "Oil change",
                mileageInterval = 5000,
                recurrence = Interval(6, IntervalUnit.MONTHS),
                reminder = Interval(5, IntervalUnit.MONTHS),
                lastDoneDate = LocalDate.of(2026, 3, 15),
                lastDoneMileage = 42000,
                lastNotifiedAt = "2026-08-01T09:00:00Z",
            ),
        ),
        serviceLogEntries = listOf(
            ServiceLogEntry(
                id = "s-1",
                vehicleId = "v-1",
                maintenanceItemId = "i-1",
                description = "Oil and filter",
                date = LocalDate.of(2026, 3, 15),
                odometer = 42000,
                cost = 6499,
            ),
        ),
    )

    @Before
    fun setUp() {
        val stamp = System.nanoTime()
        storeFile = File(context.cacheDir, "backup-store-$stamp.json")
        exported = File(context.cacheDir, "backup-export-$stamp.json")
    }

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
        exported.delete()
    }

    private fun holder() = MaintenanceStoreHolder(JsonFileStore(storeFile))

    @Test
    fun anExportedFileRestoresTheSameData() = runBlocking {
        storeFile.writeText(storeJson.encodeToString(store))
        val source = holder().also { it.load() }
        val snapshot = (JsonBackupRepository(source).exportSnapshot() as StoreResult.Success).value

        assertTrue(files.write(Uri.fromFile(exported), snapshot))

        val read = files.read(Uri.fromFile(exported))
        val parse = parseBackup((read as BackupRead.Success).json)
        assertEquals(BackupParse.Valid(store), parse)

        storeFile.writeText(storeJson.encodeToString(MaintenanceStore()))
        val target = holder().also { it.load() }
        val applied = JsonBackupRepository(target)
            .applyBackup((parse as BackupParse.Valid).store)

        assertTrue(applied is StoreResult.Success)
        assertEquals(store, target.state.value)
        assertEquals(store, storeJson.decodeFromString<MaintenanceStore>(storeFile.readText()))
    }

    @Test
    fun writingOverALongerFileLeavesNoTailBehind() = runBlocking {
        exported.writeText("x".repeat(4096))

        assertTrue(files.write(Uri.fromFile(exported), """{"schemaVersion":1}"""))

        assertEquals("""{"schemaVersion":1}""", exported.readText())
    }

    @Test
    fun aFileOverTheCapIsRefusedWithoutParsing() = runBlocking {
        exported.writeBytes(ByteArray(MAX_BACKUP_BYTES + 1))

        assertEquals(BackupRead.TooLarge, files.read(Uri.fromFile(exported)))
    }

    @Test
    fun aFileThatIsNotThereIsUnreadable() = runBlocking {
        assertEquals(BackupRead.Unreadable, files.read(Uri.fromFile(exported)))
    }
}

package com.example.vehiclemaintenance.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.vehiclemaintenance.AppContainer
import com.example.vehiclemaintenance.STORE_FILE_NAME
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.vehicles.Vehicle
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Runs the worker over a temp file, never the app's own store: the device running the tests can be
 * the one the user keeps real data on.
 */
@RunWith(AndroidJUnit4::class)
class DueReminderWorkerTest {

    private lateinit var application: VehicleMaintenanceApplication
    private lateinit var storeFile: File
    private lateinit var realStoreFile: File
    private var realStoreBefore: ByteArray? = null

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        storeFile = File(application.cacheDir, "due-reminder-test-${System.nanoTime()}.json")
        realStoreFile = File(application.filesDir, STORE_FILE_NAME)
        realStoreBefore = realStoreFile.takeIf { it.exists() }?.readBytes()
    }

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
        ReminderNotifier(application).cancel(VEHICLE_ID)
    }

    @Test
    fun dueItemIsStampedAfterTheWorkerRuns() = runBlocking {
        val overdue = LocalDate.now().minusMonths(7)
        storeFile.writeText(
            storeJson.encodeToString(
                MaintenanceStore(
                    vehicles = listOf(Vehicle(VEHICLE_ID, 2014, "Toyota", "Tacoma", "4.0L V6")),
                    maintenanceItems = listOf(
                        MaintenanceItem(
                            id = "m-1",
                            vehicleId = VEHICLE_ID,
                            name = "Oil change",
                            reminder = Interval(6, IntervalUnit.MONTHS),
                            lastDoneDate = overdue,
                        ),
                    ),
                ),
            ),
        )
        val container = AppContainer(storeFile)

        val worker = TestListenableWorkerBuilder<DueReminderWorker>(application)
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = DueReminderWorker(appContext, workerParameters, container)
                },
            )
            .build()

        assertTrue(worker.doWork() is ListenableWorker.Result.Success)

        val saved = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        val item = saved.maintenanceItems.single()
        assertEquals("m-1", item.id)
        assertNotNull("the due item should have been stamped", item.lastNotifiedAt)

        val realStoreAfter = realStoreFile.takeIf { it.exists() }?.readBytes()
        assertEquals(realStoreBefore != null, realStoreAfter != null)
        if (realStoreBefore != null) assertArrayEquals(realStoreBefore, realStoreAfter)
    }

    private companion object {
        const val VEHICLE_ID = "v-1"
    }
}

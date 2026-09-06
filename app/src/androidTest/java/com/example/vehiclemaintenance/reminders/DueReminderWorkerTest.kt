package com.example.vehiclemaintenance.reminders

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.vehiclemaintenance.STORE_FILE_NAME
import com.example.vehiclemaintenance.VehicleMaintenanceApplication
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.vehicles.Vehicle
import androidx.work.ListenableWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DueReminderWorkerTest {

    private lateinit var application: VehicleMaintenanceApplication
    private lateinit var storeFile: File

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        storeFile = File(application.filesDir, STORE_FILE_NAME)
    }

    @Test
    fun dueItemIsStampedAfterTheWorkerRuns() = runBlocking {
        val overdue = LocalDate.now().minusMonths(7)
        storeFile.writeText(
            storeJson.encodeToString(
                MaintenanceStore(
                    vehicles = listOf(Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")),
                    maintenanceItems = listOf(
                        MaintenanceItem(
                            id = "m-1",
                            vehicleId = "v-1",
                            name = "Oil change",
                            reminder = Interval(6, IntervalUnit.MONTHS),
                            lastDoneDate = overdue,
                        ),
                    ),
                ),
            ),
        )

        val worker = TestListenableWorkerBuilder<DueReminderWorker>(application).build()

        assertTrue(worker.doWork() is ListenableWorker.Result.Success)

        val saved = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        val item = saved.maintenanceItems.single()
        assertEquals("m-1", item.id)
        assertNotNull("the due item should have been stamped", item.lastNotifiedAt)
    }
}

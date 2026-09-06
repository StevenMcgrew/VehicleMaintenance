package com.example.vehiclemaintenance.servicelog

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.formatMileage
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.Vehicle
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Reads the history through the real repositories over a temp file, so ordering is proven against
 * stored data rather than a hand-built list.
 */
@RunWith(AndroidJUnit4::class)
class ServiceHistoryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var storeFile: File
    private lateinit var vehicles: VehicleRepository
    private lateinit var serviceLog: ServiceLogRepository

    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")
    private val otherVehicle = Vehicle("v-2", 2019, "Honda", "Civic", "2.0L I4")

    private val oldest = ServiceLogEntry(
        id = "s-1",
        vehicleId = "v-1",
        maintenanceItemId = "m-1",
        description = "Tire rotation",
        date = LocalDate.of(2026, 3, 1),
        odometer = 42000,
    )
    private val sameDayFirst = ServiceLogEntry(
        id = "s-2",
        vehicleId = "v-1",
        description = "Replaced the alternator",
        date = LocalDate.of(2026, 9, 5),
        odometer = 48000,
        cost = 78250,
    )
    private val sameDaySecond = ServiceLogEntry(
        id = "s-3",
        vehicleId = "v-1",
        maintenanceItemId = "m-1",
        description = "Oil and filter",
        date = LocalDate.of(2026, 9, 5),
        odometer = 48010,
        cost = 6499,
        notes = "Shop said the belts look fine",
    )
    private val otherVehicleEntry = ServiceLogEntry(
        id = "s-4",
        vehicleId = "v-2",
        description = "Brake pads",
        date = LocalDate.of(2026, 8, 1),
        odometer = 21000,
    )

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
    }

    private fun seed(store: MaintenanceStore) {
        storeFile = File(context.cacheDir, "history-test-${System.nanoTime()}.json")
        storeFile.writeText(storeJson.encodeToString(store))
        val holder = MaintenanceStoreHolder(JsonFileStore(storeFile))
        vehicles = JsonVehicleRepository(holder)
        serviceLog = JsonServiceLogRepository(holder)
    }

    private fun seedFullHistory() = seed(
        MaintenanceStore(
            vehicles = listOf(vehicle, otherVehicle),
            serviceLogEntries = listOf(oldest, sameDayFirst, sameDaySecond, otherVehicleEntry),
        ),
    )

    @Test
    fun historyShowsOnlyTheEntriesForThisVehicle() {
        seedFullHistory()
        setContent()

        waitForText(sameDaySecond.description)
        composeRule.onNodeWithText(oldest.description).assertIsDisplayed()
        composeRule.onNodeWithText(sameDayFirst.description).assertIsDisplayed()
        composeRule.onAllNodesWithText(otherVehicleEntry.description).assertCountEquals(0)
    }

    @Test
    fun historyReadsNewestFirstWithTheLatestLogAtTheTop() {
        seedFullHistory()
        setContent()
        waitForText(sameDaySecond.description)

        val tops = listOf(sameDaySecond, sameDayFirst, oldest).map { entry ->
            composeRule.onNodeWithText(entry.description)
                .fetchSemanticsNode().positionInRoot.y
        }

        assert(tops == tops.sorted()) {
            "expected newest first, got rows at $tops"
        }
    }

    @Test
    fun anEntryShowsItsDateOdometerCostAndNotes() {
        seedFullHistory()
        setContent()
        waitForText(sameDaySecond.description)

        composeRule.onAllNodesWithText("2026-09-05").assertCountEquals(2)
        composeRule
            .onNodeWithText(odometerText(48010))
            .assertIsDisplayed()
        composeRule.onNodeWithText(formatCost(6499)).assertIsDisplayed()
        composeRule.onNodeWithText(sameDaySecond.notes!!).assertIsDisplayed()
    }

    @Test
    fun anEntryWithoutCostOrNotesShowsNeither() {
        seed(
            MaintenanceStore(vehicles = listOf(vehicle), serviceLogEntries = listOf(oldest)),
        )
        setContent()

        waitForText(oldest.description)
        composeRule.onAllNodesWithText(formatCost(0)).assertCountEquals(0)
        composeRule.onNodeWithText(odometerText(42000)).assertIsDisplayed()
    }

    @Test
    fun aVehicleWithNothingLoggedShowsTheEmptyState() {
        seed(MaintenanceStore(vehicles = listOf(vehicle)))
        setContent()

        waitForText(string(R.string.history_empty_title))
        composeRule.onNodeWithText(string(R.string.history_empty_body)).assertIsDisplayed()
    }

    private fun setContent() {
        composeRule.setContent {
            VehicleMaintenanceTheme {
                val historyViewModel = remember {
                    ServiceHistoryViewModel(vehicles, serviceLog, "v-1")
                }
                ServiceHistoryScreen(
                    vehicleId = "v-1",
                    onBack = {},
                    viewModel = historyViewModel,
                )
            }
        }
    }

    private fun odometerText(miles: Int): String =
        context.getString(R.string.history_odometer, formatMileage(miles))

    private fun string(id: Int): String = context.getString(id)

    private fun waitForText(text: String) {
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}

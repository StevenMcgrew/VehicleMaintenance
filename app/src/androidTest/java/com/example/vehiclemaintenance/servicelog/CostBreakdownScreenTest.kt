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

/** Reads the breakdown through the real repositories over a temp file, as the history test does. */
@RunWith(AndroidJUnit4::class)
class CostBreakdownScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var storeFile: File
    private lateinit var vehicles: VehicleRepository
    private lateinit var serviceLog: ServiceLogRepository

    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")
    private val otherVehicle = Vehicle("v-2", 2019, "Honda", "Civic", "2.0L I4")

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
    }

    private fun seed(entries: List<ServiceLogEntry>) {
        storeFile = File(context.cacheDir, "costs-test-${System.nanoTime()}.json")
        storeFile.writeText(
            storeJson.encodeToString(
                MaintenanceStore(
                    vehicles = listOf(vehicle, otherVehicle),
                    serviceLogEntries = entries,
                ),
            ),
        )
        val holder = MaintenanceStoreHolder(JsonFileStore(storeFile))
        vehicles = JsonVehicleRepository(holder)
        serviceLog = JsonServiceLogRepository(holder)
    }

    /** 2026 totals $150.00 over two entries, 2025 totals $90.00, and the uncosted entry adds nothing. */
    private fun seedTwoYears() = seed(
        listOf(
            entry("s-1", "Oil change", LocalDate.of(2026, 4, 2), cost = 10_000),
            entry("s-2", "Wiper blades", LocalDate.of(2026, 1, 20), cost = 5_000),
            entry("s-3", "Brake pads", LocalDate.of(2025, 8, 9), cost = 9_000),
            entry("s-4", "Tire rotation", LocalDate.of(2025, 2, 1), cost = null),
            entry("s-5", "Transmission", LocalDate.of(2026, 5, 1), cost = 99_900, vehicleId = "v-2"),
        ),
    )

    @Test
    fun theBreakdownShowsTheAllTimeTotal() {
        seedTwoYears()
        setContent()

        waitForText(string(R.string.cost_all_time))
        composeRule.onNodeWithText(formatCost(24_000L)).assertIsDisplayed()
    }

    @Test
    fun eachYearShowsItsSubtotalNewestFirst() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        composeRule.onNodeWithText(formatCost(15_000L)).assertIsDisplayed()
        // 2025 has one costed entry, so its subtotal and that entry share the amount.
        composeRule.onAllNodesWithText(formatCost(9_000L)).assertCountEquals(2)
        val tops = listOf(2026, 2025).map { year ->
            composeRule.onNodeWithText(context.getString(R.string.cost_year, year))
                .fetchSemanticsNode().positionInRoot.y
        }
        assert(tops == tops.sorted()) { "expected newest year first, got headers at $tops" }
    }

    @Test
    fun eachYearListsItsCostedEntries() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        composeRule.onNodeWithText("Oil change").assertIsDisplayed()
        composeRule.onNodeWithText(formatCost(10_000)).assertIsDisplayed()
        composeRule.onNodeWithText("Wiper blades").assertIsDisplayed()
        composeRule.onNodeWithText(formatCost(5_000)).assertIsDisplayed()
        composeRule.onNodeWithText("Brake pads").assertIsDisplayed()
    }

    @Test
    fun uncostedEntriesAndOtherVehiclesAreLeftOut() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        composeRule.onAllNodesWithText("Tire rotation").assertCountEquals(0)
        composeRule.onAllNodesWithText("Transmission").assertCountEquals(0)
        composeRule.onAllNodesWithText(formatCost(99_900)).assertCountEquals(0)
    }

    @Test
    fun aVehicleWithNothingCostedSaysSoInsteadOfShowingZero() {
        seed(listOf(entry("s-1", "Tire rotation", LocalDate.of(2026, 4, 2), cost = null)))
        setContent()

        waitForText(string(R.string.cost_totals_none))
        composeRule.onNodeWithText(string(R.string.cost_breakdown_empty_body)).assertIsDisplayed()
        composeRule.onAllNodesWithText(formatCost(0L)).assertCountEquals(0)
    }

    private fun entry(
        id: String,
        description: String,
        date: LocalDate,
        cost: Int?,
        vehicleId: String = "v-1",
    ) = ServiceLogEntry(
        id = id,
        vehicleId = vehicleId,
        description = description,
        date = date,
        odometer = 48_000,
        cost = cost,
    )

    private fun setContent() {
        composeRule.setContent {
            VehicleMaintenanceTheme {
                val costsViewModel = remember {
                    CostBreakdownViewModel(vehicles, serviceLog, "v-1")
                }
                CostBreakdownScreen(
                    vehicleId = "v-1",
                    onBack = {},
                    viewModel = costsViewModel,
                )
            }
        }
    }

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

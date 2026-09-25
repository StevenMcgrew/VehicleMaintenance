package com.example.vehiclemaintenance.servicelog

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.formatMediumDate
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
 * Reads the history through the real repositories over a temp file, so ordering and totals are
 * proven against stored data rather than a hand-built list.
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

    private val oilChange = entry(
        "s-1",
        "Oil change",
        LocalDate.of(2026, 4, 2),
        odometer = 48_000,
        cost = 10_000,
        notes = "Shop said the belts look fine",
    )
    private val wipers = entry("s-2", "Wiper blades", LocalDate.of(2026, 1, 20), 46_500, 5_000)
    private val brakes = entry("s-3", "Brake pads", LocalDate.of(2025, 8, 9), 44_000, 9_000)
    private val rotation = entry("s-4", "Tire rotation", LocalDate.of(2025, 2, 1), 41_000, null)
    private val otherVehicleEntry = entry(
        "s-5",
        "Transmission",
        LocalDate.of(2026, 5, 1),
        odometer = 21_000,
        cost = 99_900,
        vehicleId = "v-2",
    )

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
    }

    private fun seed(entries: List<ServiceLogEntry>) {
        storeFile = File(context.cacheDir, "history-test-${System.nanoTime()}.json")
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

    /**
     * 2026 totals $150.00 over two entries, 2025 totals $90.00, and the uncosted rotation adds
     * nothing but starts the span. 2025-02-01 to 2026-04-02 is 425 days, so the $240.00 averages
     * 240 x 365.25 / 425 = $206.26 a year.
     */
    private fun seedTwoYears() =
        seed(listOf(oilChange, wipers, brakes, rotation, otherVehicleEntry))

    @Test
    fun theSummaryShowsTheAllTimeTotalAndTheAveragePerYear() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        composeRule.onNodeWithText(formatCost(24_000L)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cost_average_per_year)).assertIsDisplayed()
        composeRule.onNodeWithText(formatCost(20_626L)).assertIsDisplayed()
    }

    @Test
    fun theAverageInfoIconExplainsTheCalculation() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        composeRule.onNodeWithContentDescription(string(R.string.cost_average_info)).performClick()
        waitForText(string(R.string.cost_average_info_body))
        composeRule.onNodeWithText(string(R.string.ok)).performClick()

        composeRule.onAllNodesWithText(string(R.string.cost_average_info_body))
            .assertCountEquals(0)
    }

    @Test
    fun aSpanShorterThanTheMinimumShowsNoAverage() {
        seed(
            listOf(
                entry("s-1", "Oil change", LocalDate.of(2026, 4, 10), 48_000, 6_000),
                entry("s-2", "Wiper blades", LocalDate.of(2026, 4, 1), 47_800, 2_000),
            ),
        )
        setContent()
        waitForText(string(R.string.cost_average_per_year))

        // All time and the 2026 subtotal.
        composeRule.onAllNodesWithText(formatCost(8_000L)).assertCountEquals(2)
        composeRule.onNodeWithText(string(R.string.value_not_set)).assertIsDisplayed()
    }

    @Test
    fun eachYearShowsItsSubtotalNewestFirst() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        composeRule.onNodeWithText(formatCost(15_000L)).assertIsDisplayed()
        // 2025 has one costed entry, so its subtotal and that entry share the amount.
        composeRule.onAllNodesWithText(formatCost(9_000L)).assertCountEquals(2)
        val tops = listOf(2026, 2025).map { topOf(context.getString(R.string.cost_year, it)) }
        assert(tops == tops.sorted()) { "expected newest year first, got headers at $tops" }
    }

    @Test
    fun everyEntryIsListedNewestFirstWithItsDateAndMileage() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        val newestFirst = listOf(oilChange, wipers, brakes, rotation)
        newestFirst.forEach { entry ->
            composeRule.onNodeWithText(entry.description).assertIsDisplayed()
            composeRule.onNodeWithText(dateAndMileage(entry)).assertIsDisplayed()
        }
        val tops = newestFirst.map { topOf(it.description) }
        assert(tops == tops.sorted()) { "expected newest entry first, got rows at $tops" }
    }

    @Test
    fun anEntryShowsItsCostAndNotes() {
        seedTwoYears()
        setContent()
        waitForText(oilChange.description)

        composeRule.onNodeWithText(formatCost(10_000)).assertIsDisplayed()
        composeRule.onNodeWithText(oilChange.notes!!).assertIsDisplayed()
    }

    @Test
    fun otherVehiclesEntriesAreLeftOut() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))

        composeRule.onAllNodesWithText(otherVehicleEntry.description).assertCountEquals(0)
        composeRule.onAllNodesWithText(formatCost(99_900)).assertCountEquals(0)
    }

    @Test
    fun aVehicleWithNothingCostedListsItsEntriesAndSaysSo() {
        seed(listOf(rotation))
        setContent()

        waitForText(string(R.string.cost_totals_none))
        composeRule.onNodeWithText(rotation.description).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.value_not_set)).assertIsDisplayed()
        composeRule.onAllNodesWithText(string(R.string.cost_average_per_year))
            .assertCountEquals(0)
        composeRule.onAllNodesWithText(formatCost(0L)).assertCountEquals(0)
    }

    @Test
    fun aVehicleWithNothingLoggedShowsTheEmptyState() {
        seed(emptyList())
        setContent()

        waitForText(string(R.string.history_empty_title))
        composeRule.onNodeWithText(string(R.string.history_empty_body)).assertIsDisplayed()
    }

    private fun entry(
        id: String,
        description: String,
        date: LocalDate,
        odometer: Int,
        cost: Int?,
        notes: String? = null,
        vehicleId: String = "v-1",
    ) = ServiceLogEntry(
        id = id,
        vehicleId = vehicleId,
        description = description,
        date = date,
        odometer = odometer,
        cost = cost,
        notes = notes,
    )

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

    private fun dateAndMileage(entry: ServiceLogEntry): String = context.getString(
        R.string.history_date_odometer,
        formatMediumDate(entry.date),
        formatMileage(entry.odometer),
    )

    private fun topOf(text: String): Float =
        composeRule.onNodeWithText(text).fetchSemanticsNode().positionInRoot.y

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

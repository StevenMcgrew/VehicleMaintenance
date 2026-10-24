package com.example.vehiclemaintenance.servicelog

import androidx.compose.runtime.remember
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
        itemId = "m-1",
    )
    private val wipers =
        entry("s-2", "Wiper blades", LocalDate.of(2026, 1, 20), 46_500, 5_000, itemId = "m-2")
    private val brakes =
        entry("s-3", "Brake pads", LocalDate.of(2025, 8, 9), 44_000, 9_000, itemId = "m-3")
    private val rotation =
        entry("s-4", "Tire rotation", LocalDate.of(2025, 2, 1), 41_000, null, itemId = "m-4")
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
     * 240 x 365.25 / 425 = $206.26 a year. The breakdown is oil $100.00 (42%), brakes $90.00
     * (38%), and wipers $50.00 (21%); the uncosted rotation gets no slice.
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

        // All time and the 2026 subtotal; the legend's Additional repairs slice is the third.
        listNodes(formatCost(8_000L)).assertCountEquals(2)
        composeRule.onNodeWithText(string(R.string.value_not_set)).assertIsDisplayed()
    }

    @Test
    fun eachYearShowsItsSubtotalNewestFirst() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))
        scrollToHistory()

        composeRule.onNodeWithText(formatCost(15_000L)).assertIsDisplayed()
        // 2025 has one costed entry, so its subtotal and that entry share the amount.
        listNodes(formatCost(9_000L)).assertCountEquals(2)
        val tops = listOf(2026, 2025).map { topOf(context.getString(R.string.cost_year, it)) }
        assert(tops == tops.sorted()) { "expected newest year first, got headers at $tops" }
    }

    @Test
    fun everyEntryIsListedNewestFirstWithItsDateAndMileage() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))
        scrollToHistory()

        val newestFirst = listOf(oilChange, wipers, brakes, rotation)
        newestFirst.forEach { entry ->
            composeRule.onNode(inList(entry.description)).assertIsDisplayed()
            composeRule.onNodeWithText(dateAndMileage(entry)).assertIsDisplayed()
        }
        val tops = newestFirst.map { topOf(inList(it.description)) }
        assert(tops == tops.sorted()) { "expected newest entry first, got rows at $tops" }
    }

    @Test
    fun anEntryShowsItsCostAndNotes() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_all_time))
        scrollToHistory()

        composeRule.onNode(inList(formatCost(10_000))).assertIsDisplayed()
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
        composeRule.onAllNodesWithText(string(R.string.cost_breakdown)).assertCountEquals(0)
    }

    @Test
    fun aVehicleWithNothingLoggedShowsTheEmptyState() {
        seed(emptyList())
        setContent()

        waitForText(string(R.string.history_empty_title))
        composeRule.onNodeWithText(string(R.string.history_empty_body)).assertIsDisplayed()
    }

    @Test
    fun theBreakdownListsEachTrackedItemAboveTheSummary() {
        seedTwoYears()
        setContent()
        waitForText(string(R.string.cost_breakdown))

        composeRule.onNodeWithContentDescription(string(R.string.cost_breakdown_chart))
            .assertIsDisplayed()
        listOf(
            Triple(oilChange, 10_000L, 42),
            Triple(brakes, 9_000L, 38),
            Triple(wipers, 5_000L, 21),
        ).forEach { (entry, total, percent) ->
            composeRule.onNode(inBreakdown(entry.description)).assertIsDisplayed()
            composeRule.onNode(inBreakdown(formatCost(total))).assertIsDisplayed()
            composeRule.onNode(inBreakdown(percentText(percent))).assertIsDisplayed()
        }
        composeRule.onAllNodes(inBreakdown(rotation.description)).assertCountEquals(0)
        composeRule.onAllNodes(inBreakdown(string(R.string.cost_breakdown_additional_repairs)))
            .assertCountEquals(0)
        assert(topOf(string(R.string.cost_breakdown)) < topOf(string(R.string.cost_all_time))) {
            "expected the breakdown above the all time total"
        }
    }

    @Test
    fun adHocRepairsShareOneAdditionalRepairsSlice() {
        // $100.00 of oil changes against $800.00 of repairs: 11% and 89%.
        seed(
            listOf(
                entry("s-1", "Oil change", LocalDate.of(2026, 4, 2), 48_000, 6_000, itemId = "m-1"),
                entry("s-2", "Oil change", LocalDate.of(2025, 4, 2), 43_000, 4_000, itemId = "m-1"),
                entry("s-3", "Replaced the alternator", LocalDate.of(2026, 2, 1), 46_000, 78_250),
                entry("s-4", "Windshield chip", LocalDate.of(2025, 9, 1), 45_000, 1_750),
            ),
        )
        setContent()
        waitForText(string(R.string.cost_breakdown))

        composeRule.onNode(inBreakdown(string(R.string.cost_breakdown_additional_repairs)))
            .assertIsDisplayed()
        composeRule.onNode(inBreakdown(formatCost(80_000L))).assertIsDisplayed()
        composeRule.onNode(inBreakdown(percentText(89))).assertIsDisplayed()
        composeRule.onNode(inBreakdown("Oil change")).assertIsDisplayed()
        composeRule.onNode(inBreakdown(formatCost(10_000L))).assertIsDisplayed()
        composeRule.onNode(inBreakdown(percentText(11))).assertIsDisplayed()
        composeRule.onAllNodes(inBreakdown("Replaced the alternator")).assertCountEquals(0)
        composeRule.onAllNodes(inBreakdown("Windshield chip")).assertCountEquals(0)
    }

    @Test
    fun thePdfButtonOffersSavePrintAndShare() {
        seedTwoYears()
        setContent()

        waitForText(string(R.string.pdf_button))
        composeRule.onNodeWithText(string(R.string.pdf_button)).performClick()

        listOf(R.string.pdf_save, R.string.pdf_print, R.string.pdf_share).forEach {
            composeRule.onNodeWithText(string(it)).assertIsDisplayed()
        }
    }

    @Test
    fun aVehicleWithNothingLoggedHasNoPdfButton() {
        seed(emptyList())
        setContent()

        waitForText(string(R.string.history_empty_title))
        composeRule.onNodeWithText(string(R.string.pdf_button)).assertDoesNotExist()
    }

    private fun entry(
        id: String,
        description: String,
        date: LocalDate,
        odometer: Int,
        cost: Int?,
        notes: String? = null,
        vehicleId: String = "v-1",
        itemId: String? = null,
    ) = ServiceLogEntry(
        id = id,
        vehicleId = vehicleId,
        maintenanceItemId = itemId,
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
                    ServiceHistoryViewModel(vehicles, serviceLog, HistoryPdfFiles(context), "v-1")
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

    private fun topOf(text: String): Float = topOf(hasText(text))

    private fun topOf(matcher: SemanticsMatcher): Float =
        composeRule.onNode(matcher).fetchSemanticsNode().positionInRoot.y

    private fun string(id: Int): String = context.getString(id)

    private fun percentText(percent: Int): String =
        context.getString(R.string.cost_breakdown_percent, percent)

    /** [text] inside the cost breakdown legend. */
    private fun inBreakdown(text: String): SemanticsMatcher =
        hasText(text) and hasAnyAncestor(hasTestTag(COST_BREAKDOWN_TAG))

    /** [text] anywhere but the cost breakdown legend, which repeats names and amounts. */
    private fun inList(text: String): SemanticsMatcher =
        hasText(text) and !hasAnyAncestor(hasTestTag(COST_BREAKDOWN_TAG))

    private fun listNodes(text: String) = composeRule.onAllNodes(inList(text))

    /**
     * Brings the newest year to the top of the list. The breakdown above it can push the rows
     * below the fold, and a lazy list only composes what is on screen.
     */
    private fun scrollToHistory() {
        val newestYear = context.getString(R.string.cost_year, 2026)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(newestYear))
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}

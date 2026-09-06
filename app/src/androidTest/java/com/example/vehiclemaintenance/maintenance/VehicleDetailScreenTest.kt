package com.example.vehiclemaintenance.maintenance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.servicelog.JsonServiceLogRepository
import com.example.vehiclemaintenance.servicelog.ServiceLogEntry
import com.example.vehiclemaintenance.servicelog.ServiceLogRepository
import com.example.vehiclemaintenance.servicelog.formatCost
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.Vehicle
import com.example.vehiclemaintenance.vehicles.VehicleRepository
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * Drives the detail screen and the item form over real repositories backed by a temp file, so the
 * add-then-appears path goes through persistence rather than a stubbed list.
 */
@RunWith(AndroidJUnit4::class)
class VehicleDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var storeFile: File
    private lateinit var vehicles: VehicleRepository
    private lateinit var items: MaintenanceItemRepository
    private lateinit var serviceLog: ServiceLogRepository

    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")

    private var historyRequested = false

    @Before
    fun setUp() {
        storeFile = File(context.cacheDir, "detail-test-${System.nanoTime()}.json")
        seedStore()
        val holder = MaintenanceStoreHolder(JsonFileStore(storeFile))
        vehicles = JsonVehicleRepository(holder)
        items = JsonMaintenanceItemRepository(holder)
        serviceLog = JsonServiceLogRepository(holder)
    }

    /** Writes the backing file the repositories read, optionally with a seeded service log. */
    private fun seedStore(entries: List<ServiceLogEntry> = emptyList()) {
        storeFile.writeText(
            storeJson.encodeToString(
                MaintenanceStore(vehicles = listOf(vehicle), serviceLogEntries = entries),
            ),
        )
    }

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
    }

    @Test
    fun detailShowsTheEmptyStateWhenTheVehicleHasNoItems() {
        setContent()

        waitForText(string(R.string.maintenance_empty_title))
        composeRule.onNodeWithText(string(R.string.maintenance_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.add_maintenance_item)).assertIsDisplayed()
    }

    @Test
    fun detailShowsTheVehicleItBelongsTo() {
        setContent()

        val summary = context.getString(
            R.string.vehicle_summary_with_engine,
            2014,
            "Toyota",
            "Tacoma",
            "4.0L V6",
        )
        waitForText(summary)
        composeRule.onNodeWithText(summary).assertIsDisplayed()
    }

    @Test
    fun anAddedItemAppearsOnTheDetailScreen() {
        setContent()
        waitForText(string(R.string.maintenance_empty_title))

        composeRule.onNodeWithText(string(R.string.add_maintenance_item)).performClick()
        waitForText(string(R.string.save))

        // Editable field order: name, mileage interval, recurrence value, reminder value,
        // last done mileage. The unit dropdowns and the date field are read only.
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Oil change")
        fields[3].performTextInput("5")

        composeRule.onNodeWithText(string(R.string.save)).performClick()

        waitForText("Oil change")
        composeRule.onNodeWithText("Oil change").assertIsDisplayed()
    }

    @Test
    fun anAddedItemIsPersistedToTheStoreFile() {
        setContent()
        waitForText(string(R.string.maintenance_empty_title))

        composeRule.onNodeWithText(string(R.string.add_maintenance_item)).performClick()
        waitForText(string(R.string.save))
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Oil change")
        fields[3].performTextInput("5")
        composeRule.onNodeWithText(string(R.string.save)).performClick()
        waitForText("Oil change")

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        val item = onDisk.maintenanceItems.single()
        assert(item.name == "Oil change") { "expected the stored name, got ${item.name}" }
        assert(item.vehicleId == "v-1") { "expected the owning vehicle, got ${item.vehicleId}" }
        assert(item.reminder == Interval(5, IntervalUnit.MONTHS)) {
            "expected the reminder interval, got ${item.reminder}"
        }
        assert(item.lastDoneDate != null) { "creation should backfill the last done date" }
    }

    @Test
    fun theTableLabelsEveryColumnAndShowsTheStatus() {
        addOilChange()

        composeRule.onNodeWithText(string(R.string.column_service)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.column_next_due)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.column_miles_left)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.column_status)).assertIsDisplayed()

        // Created today with a five month reminder, so nothing has come due yet.
        composeRule.onNodeWithText(string(R.string.status_ok)).assertIsDisplayed()
    }

    @Test
    fun anOverdueRowNamesItsStatusAndShowsBothIntervalsUnderTheName() {
        setDetailContent(rows = listOf(overdueRow()))

        composeRule.onNodeWithText(string(R.string.status_overdue)).assertIsDisplayed()

        val subtitle = context.getString(
            R.string.interval_summary_both,
            context.getString(R.string.interval_short_miles, formatMileage(5_000)),
            context.getString(R.string.interval_short_months, 6),
        )
        composeRule.onNodeWithText(subtitle).assertIsDisplayed()
    }

    @Test
    fun anItemWithNothingComputableShowsADashInEveryDerivedColumn() {
        setDetailContent(rows = listOf(noSignalRow()))

        // The row is clickable, so its cells merge into one node unless the tree is unmerged.
        val dashes = composeRule
            .onAllNodesWithText(string(R.string.value_not_set), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assert(dashes.size == 3) { "expected next due, miles left, and status dashes, got $dashes" }
    }

    @Test
    fun aNewReadingCalloutNamesTheItemsAndDismisses() {
        var dismissed = false
        setDetailContent(
            rows = listOf(overdueRow()),
            newlyOverdue = listOf("Oil change"),
            onNewlyOverdueShown = { dismissed = true },
        )

        composeRule
            .onNode(hasText("Oil change") and hasAnyAncestor(isDialog()))
            .assertIsDisplayed()

        composeRule.onNodeWithText(string(R.string.ok)).performClick()

        assert(dismissed) { "expected the callout dismissal to fire" }
    }

    /** Overdue by mileage: due at 47,000 with the vehicle reading 48,000. */
    private fun overdueRow(): MaintenanceItemRow = row(
        MaintenanceItem(
            id = "m-1",
            vehicleId = "v-1",
            name = "Oil change",
            mileageInterval = 5_000,
            recurrence = Interval(6, IntervalUnit.MONTHS),
            reminder = Interval(5, IntervalUnit.MONTHS),
            lastDoneDate = LocalDate.of(2026, 3, 1),
            lastDoneMileage = 42_000,
        ),
        odometer = 48_000,
    )

    private fun noSignalRow(): MaintenanceItemRow = row(
        MaintenanceItem(
            id = "m-2",
            vehicleId = "v-1",
            name = "Cabin air filter",
            reminder = Interval(1, IntervalUnit.YEARS),
            lastDoneDate = null,
        ),
        odometer = 48_000,
    )

    private fun row(item: MaintenanceItem, odometer: Int?) =
        MaintenanceItemRow(item, statusOf(item, odometer, TODAY))

    /** Drives the content directly, which needs no repository and no store file. */
    private fun setDetailContent(
        rows: List<MaintenanceItemRow>,
        newlyOverdue: List<String> = emptyList(),
        onNewlyOverdueShown: () -> Unit = {},
    ) {
        composeRule.setContent {
            VehicleMaintenanceTheme {
                VehicleDetailContent(
                    uiState = VehicleDetailUiState(
                        isLoading = false,
                        vehicle = vehicle,
                        rows = rows,
                        newlyOverdueByMileage = newlyOverdue,
                    ),
                    onEditVehicle = {},
                    onAddItem = {},
                    onEditItem = {},
                    onLogService = {},
                    onLogRepair = {},
                    onViewHistory = {},
                    onDeleteItem = {},
                    onDeleteErrorShown = {},
                    onNewlyOverdueShown = onNewlyOverdueShown,
                    onRetry = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun theActionRowOffersHistoryLogRepairAndEditFromTheBody() {
        setContent()
        waitForText(string(R.string.service_history))

        composeRule.onNodeWithText(string(R.string.service_history)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.log_repair)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.edit_vehicle)).assertIsDisplayed()
    }

    @Test
    fun tappingServiceHistoryAsksToOpenTheHistory() {
        setContent()
        waitForText(string(R.string.service_history))

        composeRule.onNodeWithText(string(R.string.service_history)).performClick()

        assert(historyRequested) { "expected the history callback to fire" }
    }

    @Test
    fun deletingFromTheItemFormRemovesTheRowAndTheStoredItem() {
        addOilChange()

        openItemActions()
        composeRule.onNodeWithText(string(R.string.edit_maintenance_item)).performClick()
        waitForText(string(R.string.save))

        composeRule.onNodeWithText(string(R.string.delete)).performClick()
        waitForText(context.getString(R.string.delete_item_title, "Oil change"))
        composeRule
            .onNode(hasText(string(R.string.delete)) and hasAnyAncestor(isDialog()))
            .performClick()

        waitForText(string(R.string.maintenance_empty_title))

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        assert(onDisk.maintenanceItems.isEmpty()) {
            "expected no stored items, got ${onDisk.maintenanceItems}"
        }
    }

    @Test
    fun tappingARowOffersLogEditAndDelete() {
        addOilChange()

        openItemActions()

        composeRule.onNodeWithText(string(R.string.item_action_log_service)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.edit_maintenance_item)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.delete_maintenance_item)).assertIsDisplayed()
    }

    @Test
    fun deletingFromTheActionsSheetRemovesTheRowAndTheStoredItem() {
        addOilChange()

        openItemActions()
        composeRule.onNodeWithText(string(R.string.delete_maintenance_item)).performClick()
        waitForText(context.getString(R.string.delete_item_title, "Oil change"))
        composeRule
            .onNode(hasText(string(R.string.delete)) and hasAnyAncestor(isDialog()))
            .performClick()

        waitForText(string(R.string.maintenance_empty_title))

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        assert(onDisk.maintenanceItems.isEmpty()) {
            "expected no stored items, got ${onDisk.maintenanceItems}"
        }
    }

    @Test
    fun theDetailScreenTotalsTheCostOfEveryLoggedService() {
        seedStore(twoYearLog())
        setContent()

        val allTime = formatCost(24_000L)
        waitForText(allTime)
        composeRule.onNodeWithText(string(R.string.cost_total_label)).assertIsDisplayed()
        composeRule.onNodeWithText(allTime).assertIsDisplayed()
    }

    @Test
    fun theTotalsRowOpensAPerYearBreakdown() {
        seedStore(twoYearLog())
        setContent()
        waitForText(formatCost(24_000L))

        composeRule.onNodeWithText(string(R.string.cost_total_label)).performClick()
        waitForText(string(R.string.cost_totals_title))

        composeRule.onNodeWithText(context.getString(R.string.cost_year, 2026)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.cost_year, 2025)).assertIsDisplayed()
        composeRule.onNodeWithText(formatCost(15_000L)).assertIsDisplayed()
        composeRule.onNodeWithText(formatCost(9_000L)).assertIsDisplayed()
    }

    @Test
    fun aVehicleWhoseServicesHaveNoCostSaysSoInsteadOfShowingZero() {
        seedStore(
            listOf(
                logEntry("s-1", LocalDate.of(2026, 4, 2), cost = null),
                logEntry("s-2", LocalDate.of(2025, 8, 9), cost = null),
            ),
        )
        setContent()

        waitForText(string(R.string.cost_totals_none))
        composeRule.onNodeWithText(string(R.string.cost_totals_none)).assertIsDisplayed()
        composeRule.onNodeWithText(formatCost(0L)).assertDoesNotExist()
    }

    /** 2026 totals $150.00, 2025 totals $90.00, and the 2025 uncosted entry adds nothing. */
    private fun twoYearLog(): List<ServiceLogEntry> = listOf(
        logEntry("s-1", LocalDate.of(2026, 4, 2), cost = 15_000),
        logEntry("s-2", LocalDate.of(2025, 8, 9), cost = 9_000),
        logEntry("s-3", LocalDate.of(2025, 2, 1), cost = null),
    )

    private fun logEntry(id: String, date: LocalDate, cost: Int?) = ServiceLogEntry(
        id = id,
        vehicleId = "v-1",
        description = "Oil change",
        date = date,
        odometer = 48_000,
        cost = cost,
    )

    /** A row tap now opens the actions sheet rather than going straight to the form. */
    private fun openItemActions() {
        composeRule.onNodeWithText("Oil change").performClick()
        waitForText(string(R.string.item_action_log_service))
    }

    /** Creates one item through the real form so the detail screen renders persisted data. */
    private fun addOilChange() {
        setContent()
        waitForText(string(R.string.maintenance_empty_title))

        composeRule.onNodeWithText(string(R.string.add_maintenance_item)).performClick()
        waitForText(string(R.string.save))
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Oil change")
        fields[3].performTextInput("5")
        composeRule.onNodeWithText(string(R.string.save)).performClick()

        waitForText("Oil change")
    }

    private fun setContent() {
        composeRule.setContent {
            VehicleMaintenanceTheme {
                Harness()
            }
        }
    }

    @Composable
    private fun Harness() {
        var showForm by remember { mutableStateOf(false) }
        var editingItemId by remember { mutableStateOf<String?>(null) }
        if (showForm) {
            val formViewModel = remember(editingItemId) {
                MaintenanceItemFormViewModel(items, "v-1", editingItemId)
            }
            MaintenanceItemFormScreen(
                vehicleId = "v-1",
                itemId = editingItemId,
                onDone = { showForm = false },
                viewModel = formViewModel,
            )
        } else {
            val detailViewModel = remember {
                VehicleDetailViewModel(vehicles, items, serviceLog, "v-1")
            }
            VehicleDetailScreen(
                vehicleId = "v-1",
                onEditVehicle = {},
                onAddItem = {
                    editingItemId = null
                    showForm = true
                },
                onEditItem = {
                    editingItemId = it
                    showForm = true
                },
                onLogService = {},
                onLogRepair = {},
                onViewHistory = { historyRequested = true },
                onBack = {},
                viewModel = detailViewModel,
            )
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
        val TODAY: LocalDate = LocalDate.of(2026, 9, 6)
    }
}

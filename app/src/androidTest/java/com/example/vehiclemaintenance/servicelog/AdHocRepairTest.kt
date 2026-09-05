package com.example.vehiclemaintenance.servicelog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
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
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.JsonMaintenanceItemRepository
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.maintenance.MaintenanceItemRepository
import com.example.vehiclemaintenance.maintenance.VehicleDetailScreen
import com.example.vehiclemaintenance.maintenance.VehicleDetailViewModel
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
 * Drives the ad-hoc repair path from the detail screen through the real repositories over a temp
 * file, so the unlinked entry is proven on disk rather than in a stubbed list.
 */
@RunWith(AndroidJUnit4::class)
class AdHocRepairTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var storeFile: File
    private lateinit var vehicles: VehicleRepository
    private lateinit var items: MaintenanceItemRepository
    private lateinit var serviceLog: ServiceLogRepository

    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")
    private val item = MaintenanceItem(
        id = "m-1",
        vehicleId = "v-1",
        name = "Oil change",
        mileageInterval = 5000,
        reminder = Interval(5, IntervalUnit.MONTHS),
    )

    @Before
    fun setUp() {
        storeFile = File(context.cacheDir, "repair-test-${System.nanoTime()}.json")
        storeFile.writeText(
            storeJson.encodeToString(
                MaintenanceStore(vehicles = listOf(vehicle), maintenanceItems = listOf(item)),
            ),
        )
        val holder = MaintenanceStoreHolder(JsonFileStore(storeFile))
        vehicles = JsonVehicleRepository(holder)
        items = JsonMaintenanceItemRepository(holder)
        serviceLog = JsonServiceLogRepository(holder)
    }

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
    }

    @Test
    fun theRepairFormOpensWithItsOwnTitleAndAnEmptyDescription() {
        openRepairForm()

        composeRule.onNodeWithText(string(R.string.log_repair_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.log_repair_description_placeholder))
            .assertIsDisplayed()
    }

    @Test
    fun aLoggedRepairIsStoredWithNoItemLinkAndLeavesTheItemAlone() {
        openRepairForm()

        // Editable field order: description, odometer, cost, notes. The date field is read only
        // and already holds today.
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("Replaced the alternator")
        fields[1].performTextInput("51000")
        fields[2].performTextInput("320.50")

        composeRule.onNodeWithText(string(R.string.save)).performClick()
        waitForText("Oil change")

        val onDisk = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        val entry = onDisk.serviceLogEntries.single()
        assert(entry.maintenanceItemId == null) {
            "expected an unlinked entry, got ${entry.maintenanceItemId}"
        }
        assert(entry.vehicleId == "v-1") { "expected the owning vehicle, got ${entry.vehicleId}" }
        assert(entry.description == "Replaced the alternator") {
            "expected the typed description, got ${entry.description}"
        }
        assert(entry.odometer == 51000) { "expected the typed odometer, got ${entry.odometer}" }
        assert(entry.cost == 32050) { "expected minor units, got ${entry.cost}" }
        assert(entry.date == LocalDate.now()) { "expected today, got ${entry.date}" }
        assert(onDisk.maintenanceItems.single() == item) {
            "a repair must not reset any item, got ${onDisk.maintenanceItems.single()}"
        }
    }

    private fun openRepairForm() {
        setContent()
        waitForText("Oil change")

        composeRule.onNodeWithText(string(R.string.log_repair)).performClick()
        waitForText(string(R.string.log_repair_title))
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
        var showRepairForm by remember { mutableStateOf(false) }
        if (showRepairForm) {
            val formViewModel = remember {
                ServiceLogFormViewModel(items, vehicles, serviceLog, "v-1", null)
            }
            ServiceLogFormScreen(
                vehicleId = "v-1",
                itemId = null,
                onDone = { showRepairForm = false },
                viewModel = formViewModel,
            )
        } else {
            val detailViewModel = remember { VehicleDetailViewModel(vehicles, items, "v-1") }
            VehicleDetailScreen(
                vehicleId = "v-1",
                onEditVehicle = {},
                onAddItem = {},
                onEditItem = {},
                onLogService = {},
                onLogRepair = { showRepairForm = true },
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
    }
}

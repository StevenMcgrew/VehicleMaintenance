package com.example.vehiclemaintenance.maintenance

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

    private val vehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")

    @Before
    fun setUp() {
        storeFile = File(context.cacheDir, "detail-test-${System.nanoTime()}.json")
        storeFile.writeText(
            storeJson.encodeToString(MaintenanceStore(vehicles = listOf(vehicle))),
        )
        val holder = MaintenanceStoreHolder(JsonFileStore(storeFile))
        vehicles = JsonVehicleRepository(holder)
        items = JsonMaintenanceItemRepository(holder)
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

        val summary = context.getString(R.string.vehicle_summary, 2014, "Toyota", "Tacoma")
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
        if (showForm) {
            val formViewModel = remember {
                MaintenanceItemFormViewModel(items, "v-1", null)
            }
            MaintenanceItemFormScreen(
                vehicleId = "v-1",
                itemId = null,
                onDone = { showForm = false },
                viewModel = formViewModel,
            )
        } else {
            val detailViewModel = remember { VehicleDetailViewModel(vehicles, items, "v-1") }
            VehicleDetailScreen(
                vehicleId = "v-1",
                onEditVehicle = {},
                onAddItem = { showForm = true },
                onEditItem = {},
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

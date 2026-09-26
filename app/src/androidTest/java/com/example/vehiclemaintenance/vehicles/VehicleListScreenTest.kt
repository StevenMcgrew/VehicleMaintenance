package com.example.vehiclemaintenance.vehicles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.data.storeJson
import com.example.vehiclemaintenance.maintenance.formatMileage
import com.example.vehiclemaintenance.servicelog.JsonServiceLogRepository
import com.example.vehiclemaintenance.servicelog.ServiceLogRepository
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Drives the list and form screens over a real repository backed by a temp file, so the
 * add-then-appears path is exercised through persistence rather than a stubbed in-memory list.
 */
@RunWith(AndroidJUnit4::class)
class VehicleListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var storeFile: File
    private lateinit var repository: VehicleRepository
    private lateinit var serviceLog: ServiceLogRepository

    @Before
    fun setUp() {
        storeFile = File(context.cacheDir, "vehicle-list-test-${System.nanoTime()}.json")
        val holder = MaintenanceStoreHolder(JsonFileStore(storeFile))
        repository = JsonVehicleRepository(holder)
        serviceLog = JsonServiceLogRepository(holder)
    }

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
    }

    @Test
    fun listShowsEmptyStateWhenNoVehiclesAreStored() {
        setContent()

        waitForText(R.string.vehicles_empty_title)
        composeRule.onNodeWithText(string(R.string.vehicles_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.add_vehicle)).assertIsDisplayed()
    }

    @Test
    fun addedVehicleAppearsInTheList() {
        addTacoma()

        composeRule.onNodeWithText(headline).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.last_recorded_mileage_none)).assertIsDisplayed()
    }

    @Test
    fun aMileageEnteredOnTheFormShowsUnderTheVehicle() {
        addTacoma(mileage = "45000")

        composeRule.onNodeWithText(mileageText(45_000)).assertIsDisplayed()
        val stored = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        assertEquals(45_000, stored.vehicles.single().recordedMileage)
    }

    @Test
    fun editingToALowerMileageSavesOnlyAfterTheWarningIsAccepted() {
        val summary = addTacoma(mileage = "45000")
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.edit_vehicle_action, summary))
            .performClick()
        waitForContentDescription(R.string.save)

        val mileageField = composeRule.onAllNodes(hasSetTextAction())[4]
        mileageField.performTextClearance()
        mileageField.performTextInput("44000")
        composeRule.onNodeWithContentDescription(string(R.string.save)).performClick()

        waitForText(R.string.lower_mileage_title)
        composeRule.onNodeWithText(string(R.string.cancel)).performClick()
        composeRule.onNodeWithText(string(R.string.lower_mileage_title)).assertDoesNotExist()
        val unchanged = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        assertEquals(45_000, unchanged.vehicles.single().recordedMileage)

        composeRule.onNodeWithContentDescription(string(R.string.save)).performClick()
        waitForText(R.string.lower_mileage_title)
        composeRule.onNodeWithText(string(R.string.ok)).performClick()

        val lowered = mileageText(44_000)
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(lowered).fetchSemanticsNodes().isNotEmpty()
        }
        val stored = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        assertEquals(44_000, stored.vehicles.single().recordedMileage)
    }

    @Test
    fun rowEditIconOpensThatVehicleForEditing() {
        var editedId: String? = null
        val summary = addTacoma(onEditVehicle = { editedId = it })

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.edit_vehicle_action, summary))
            .performClick()

        val stored = storeJson.decodeFromString<MaintenanceStore>(storeFile.readText())
        assertEquals(stored.vehicles.single().id, editedId)
    }

    @Test
    fun rowDeleteIconAsksToConfirmDeletingThatVehicle() {
        val summary = addTacoma()

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.delete_vehicle_action, summary))
            .performClick()

        composeRule
            .onNodeWithText(context.getString(R.string.delete_vehicle_title, summary))
            .assertIsDisplayed()
    }

    @Test
    fun backupDataButtonOpensBackup() {
        var backupOpened = 0
        setContent(onOpenBackup = { backupOpened++ })
        waitForText(R.string.vehicles_empty_title)

        composeRule.onNodeWithText(string(R.string.backup_action)).performClick()

        assertEquals(1, backupOpened)
    }

    private val headline: String
        get() = context.getString(
            R.string.vehicle_summary_with_engine,
            2014,
            "Toyota",
            "Tacoma",
            "4.0L V6",
        )

    private fun mileageText(miles: Int): String =
        context.getString(R.string.last_recorded_mileage, formatMileage(miles))

    /**
     * Adds a vehicle through the real form and returns the label its row actions are named with,
     * once the row shows.
     */
    private fun addTacoma(onEditVehicle: (String) -> Unit = {}, mileage: String? = null): String {
        setContent(onEditVehicle = onEditVehicle)
        waitForText(R.string.vehicles_empty_title)

        composeRule.onNodeWithText(string(R.string.add_vehicle)).performClick()
        waitForContentDescription(R.string.save)

        // Field order on the form: year, make, model, engine, mileage.
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[0].performTextInput("2014")
        fields[1].performTextInput("Toyota")
        fields[2].performTextInput("Tacoma")
        fields[3].performTextInput("4.0L V6")
        mileage?.let { fields[4].performTextInput(it) }

        composeRule.onNodeWithContentDescription(string(R.string.save)).performClick()

        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(headline).fetchSemanticsNodes().isNotEmpty()
        }
        return context.getString(R.string.vehicle_summary, 2014, "Toyota", "Tacoma")
    }

    private fun setContent(
        onOpenBackup: () -> Unit = {},
        onEditVehicle: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            VehicleMaintenanceTheme {
                Harness(onOpenBackup, onEditVehicle)
            }
        }
    }

    @Composable
    private fun Harness(onOpenBackup: () -> Unit, onEditVehicle: (String) -> Unit) {
        var showForm by remember { mutableStateOf(false) }
        var editingId by remember { mutableStateOf<String?>(null) }
        if (showForm) {
            val formViewModel = remember(editingId) {
                VehicleFormViewModel(repository, serviceLog, editingId)
            }
            VehicleFormScreen(
                vehicleId = editingId,
                onDone = { showForm = false },
                viewModel = formViewModel,
            )
        } else {
            val listViewModel = remember { VehicleListViewModel(repository, serviceLog) }
            VehicleListScreen(
                onAddVehicle = {
                    editingId = null
                    showForm = true
                },
                onOpenVehicle = {},
                onEditVehicle = {
                    onEditVehicle(it)
                    editingId = it
                    showForm = true
                },
                onOpenBackup = onOpenBackup,
                viewModel = listViewModel,
            )
        }
    }

    private fun string(id: Int): String = context.getString(id)

    private fun waitForText(id: Int) {
        val text = string(id)
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForContentDescription(id: Int) {
        val description = string(id)
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}

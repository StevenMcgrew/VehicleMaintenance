package com.example.vehiclemaintenance.vehicles

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
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import org.junit.After
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

    @Before
    fun setUp() {
        storeFile = File(context.cacheDir, "vehicle-list-test-${System.nanoTime()}.json")
        repository = JsonVehicleRepository(JsonFileStore(storeFile))
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
        setContent()
        waitForText(R.string.vehicles_empty_title)

        composeRule.onNodeWithText(string(R.string.add_vehicle)).performClick()
        waitForText(R.string.save)

        // Field order on the form: nickname, year, make, model, engine.
        val fields = composeRule.onAllNodes(hasSetTextAction())
        fields[1].performTextInput("2014")
        fields[2].performTextInput("Toyota")
        fields[3].performTextInput("Tacoma")
        fields[4].performTextInput("4.0L V6")

        composeRule.onNodeWithText(string(R.string.save)).performClick()

        val summary = context.getString(R.string.vehicle_summary, 2014, "Toyota", "Tacoma")
        composeRule.waitUntil(TIMEOUT_MS) {
            composeRule.onAllNodesWithText(summary).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(summary).assertIsDisplayed()
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
            val formViewModel = remember { VehicleFormViewModel(repository, null) }
            VehicleFormScreen(
                vehicleId = null,
                onDone = { showForm = false },
                viewModel = formViewModel,
            )
        } else {
            val listViewModel = remember { VehicleListViewModel(repository) }
            VehicleListScreen(
                onAddVehicle = { showForm = true },
                onEditVehicle = {},
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

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}

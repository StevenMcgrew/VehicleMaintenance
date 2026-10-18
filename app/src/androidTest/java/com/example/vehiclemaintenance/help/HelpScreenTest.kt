package com.example.vehiclemaintenance.help

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.data.JsonFileStore
import com.example.vehiclemaintenance.data.MaintenanceStoreHolder
import com.example.vehiclemaintenance.servicelog.JsonServiceLogRepository
import com.example.vehiclemaintenance.settings.ThemeMode
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.JsonVehicleRepository
import com.example.vehiclemaintenance.vehicles.VehicleListScreen
import com.example.vehiclemaintenance.vehicles.VehicleListViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opens help from the vehicle list menu over a temp-file store, never the app's real one. */
@RunWith(AndroidJUnit4::class)
class HelpScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var storeFile: File

    @Before
    fun setUp() {
        storeFile = File(context.cacheDir, "help-test-${System.nanoTime()}.json")
    }

    @After
    fun tearDown() {
        storeFile.delete()
        File(storeFile.parentFile, storeFile.name + ".tmp").delete()
    }

    @Test
    fun helpOpensFromTheMenuAndBackReturns() {
        setContent()
        waitForText(R.string.vehicles_empty_title)

        openHelpFromTheMenu()

        composeRule.onNodeWithText(string(R.string.help_title)).assertIsDisplayed()
        composeRule.onNode(
            hasText(string(R.string.help_getting_started_heading)) and isHeading(),
        ).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(string(R.string.back)).performClick()

        composeRule.onNodeWithText(string(R.string.vehicles_title)).assertIsDisplayed()
    }

    @Test
    fun helpStaysReachableWhenTheStoreCannotBeRead() {
        storeFile.writeText("not a store")
        setContent()
        waitForText(R.string.vehicles_load_error)

        openHelpFromTheMenu()

        composeRule.onNodeWithText(string(R.string.help_title)).assertIsDisplayed()
    }

    private fun setContent() {
        val holder = MaintenanceStoreHolder(JsonFileStore(storeFile))
        val listViewModel = VehicleListViewModel(
            JsonVehicleRepository(holder),
            JsonServiceLogRepository(holder),
        )
        composeRule.setContent {
            VehicleMaintenanceTheme {
                Harness(listViewModel)
            }
        }
    }

    @Composable
    private fun Harness(listViewModel: VehicleListViewModel) {
        var showHelp by remember { mutableStateOf(false) }
        if (showHelp) {
            HelpScreen(onBack = { showHelp = false })
        } else {
            VehicleListScreen(
                onAddVehicle = {},
                onOpenVehicle = {},
                onEditVehicle = {},
                themeMode = ThemeMode.SYSTEM,
                onThemeModeChange = {},
                onOpenBackup = {},
                onOpenPrivacy = {},
                onOpenHelp = { showHelp = true },
                viewModel = listViewModel,
            )
        }
    }

    private fun openHelpFromTheMenu() {
        composeRule.onNodeWithContentDescription(string(R.string.more_options)).performClick()
        composeRule.onNodeWithText(string(R.string.help_action)).performClick()
    }

    private fun isHeading(): SemanticsMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

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

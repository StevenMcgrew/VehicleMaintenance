package com.example.vehiclemaintenance.backup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.maintenance.Interval
import com.example.vehiclemaintenance.maintenance.IntervalUnit
import com.example.vehiclemaintenance.maintenance.MaintenanceItem
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Drives the stateless content, so the system file picker stays outside the tested surface. */
@RunWith(AndroidJUnit4::class)
class BackupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val pending = MaintenanceStore(
        vehicles = listOf(
            Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6"),
            Vehicle("v-2", 2020, "Honda", "Civic", "2.0L I4"),
        ),
        maintenanceItems = listOf(
            MaintenanceItem(
                id = "i-1",
                vehicleId = "v-1",
                name = "Oil change",
                reminder = Interval(5, IntervalUnit.MONTHS),
            ),
        ),
    )

    private var exports = 0
    private var imports = 0
    private var confirms = 0
    private var cancels = 0

    @Test
    fun bothActionsAreOfferedAtRest() {
        setContent(BackupUiState())

        composeRule.onNodeWithText(string(R.string.backup_export)).assertIsEnabled()
        composeRule.onNodeWithText(string(R.string.backup_import)).assertIsEnabled()

        composeRule.onNodeWithText(string(R.string.backup_export)).performClick()
        composeRule.onNodeWithText(string(R.string.backup_import)).performClick()

        assertEquals(1, exports)
        assertEquals(1, imports)
    }

    @Test
    fun bothActionsAreDisabledWhileWorkIsInFlight() {
        setContent(BackupUiState(isBusy = true))

        composeRule.onNodeWithText(string(R.string.backup_export)).assertIsNotEnabled()
        composeRule.onNodeWithText(string(R.string.backup_import)).assertIsNotEnabled()
    }

    @Test
    fun theConfirmationNamesWhatIsRestoredAndWhatIsLost() {
        setContent(BackupUiState(pendingImport = pending))

        val counts = context.getString(
            R.string.import_confirm_counts,
            context.resources.getQuantityString(R.plurals.import_vehicle_count, 2, 2),
            context.resources.getQuantityString(R.plurals.import_item_count, 1, 1),
            context.resources.getQuantityString(R.plurals.import_record_count, 0, 0),
        )
        composeRule.onNodeWithText(string(R.string.import_confirm_title)).assertIsDisplayed()
        composeRule.onNodeWithText(counts).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.import_confirm_warning)).assertIsDisplayed()
    }

    @Test
    fun cancellingTheConfirmationRestoresNothing() {
        setContent(BackupUiState(pendingImport = pending))

        composeRule.onNodeWithText(string(R.string.cancel)).performClick()

        assertEquals(1, cancels)
        assertEquals(0, confirms)
    }

    @Test
    fun confirmingTheDialogRestoresTheFile() {
        setContent(BackupUiState(pendingImport = pending))

        composeRule.onNodeWithText(string(R.string.import_confirm_action)).performClick()

        assertEquals(1, confirms)
        assertEquals(0, cancels)
    }

    private fun setContent(uiState: BackupUiState) {
        composeRule.setContent {
            VehicleMaintenanceTheme {
                BackupContent(
                    uiState = uiState,
                    onExport = { exports++ },
                    onImport = { imports++ },
                    onConfirmImport = { confirms++ },
                    onCancelImport = { cancels++ },
                    onMessageShown = {},
                    onBack = {},
                )
            }
        }
    }

    private fun string(id: Int): String = context.getString(id)
}

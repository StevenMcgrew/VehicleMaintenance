package com.example.vehiclemaintenance.maintenance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.ChooseDateButton
import com.example.vehiclemaintenance.ui.FormCellText
import com.example.vehiclemaintenance.ui.FormDatePickerDialog
import com.example.vehiclemaintenance.ui.FormTable
import com.example.vehiclemaintenance.ui.FormTextField
import com.example.vehiclemaintenance.ui.describedAs
import com.example.vehiclemaintenance.ui.spansExtraColumn
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import java.time.LocalDate

@Composable
fun MaintenanceItemFormScreen(
    vehicleId: String,
    itemId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MaintenanceItemFormViewModel = viewModel(
        key = itemId ?: "new-item-$vehicleId",
        factory = MaintenanceItemFormViewModel.factory(vehicleId, itemId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val finishSave = rememberNotificationPermissionGate(onDone)

    LaunchedEffect(uiState.savedSuccessfully, uiState.deletedSuccessfully) {
        when {
            uiState.savedSuccessfully -> finishSave()
            uiState.deletedSuccessfully -> onDone()
        }
    }

    MaintenanceItemFormContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onMileageIntervalChange = viewModel::onMileageIntervalChange,
        onRecurrenceValueChange = viewModel::onRecurrenceValueChange,
        onRecurrenceUnitChange = viewModel::onRecurrenceUnitChange,
        onReminderValueChange = viewModel::onReminderValueChange,
        onReminderUnitChange = viewModel::onReminderUnitChange,
        onLastDoneDateChange = viewModel::onLastDoneDateChange,
        onLastDoneMileageChange = viewModel::onLastDoneMileageChange,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onCancel = onDone,
        onSaveErrorShown = viewModel::dismissSaveError,
        onDeleteErrorShown = viewModel::dismissDeleteError,
        modifier = modifier,
    )
}

/**
 * Saving the first item is the moment a reminder becomes real, which is where asking for the
 * notification permission explains itself. Either answer continues to [onDone]; Android stops
 * showing the dialog itself once the user has refused, so no "already asked" flag is stored.
 */
@Composable
private fun rememberNotificationPermissionGate(onDone: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnDone by rememberUpdatedState(onDone)
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { currentOnDone() }
    return {
        if (needsNotificationPermission(context)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            currentOnDone()
        }
    }
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceItemFormContent(
    uiState: MaintenanceItemFormUiState,
    onNameChange: (String) -> Unit,
    onMileageIntervalChange: (String) -> Unit,
    onRecurrenceValueChange: (String) -> Unit,
    onRecurrenceUnitChange: (IntervalUnit?) -> Unit,
    onReminderValueChange: (String) -> Unit,
    onReminderUnitChange: (IntervalUnit?) -> Unit,
    onLastDoneDateChange: (LocalDate?) -> Unit,
    onLastDoneMileageChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onSaveErrorShown: () -> Unit,
    onDeleteErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingDeletion by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.item_save_failed)
    val deleteFailedMessage = stringResource(R.string.delete_item_failed)

    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) {
            snackbarHostState.showSnackbar(saveFailedMessage)
            onSaveErrorShown()
        }
    }

    LaunchedEffect(uiState.deleteFailed) {
        if (uiState.deleteFailed) {
            snackbarHostState.showSnackbar(deleteFailedMessage)
            onDeleteErrorShown()
        }
    }

    val actionsEnabled = !uiState.isLoading &&
        !uiState.isSaving &&
        !uiState.isDeleting &&
        !uiState.itemNotFound

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) {
                                R.string.edit_maintenance_item
                            } else {
                                R.string.add_maintenance_item
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing && !uiState.itemNotFound) {
                        IconButton(
                            onClick = { confirmingDeletion = true },
                            enabled = actionsEnabled,
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete),
                            )
                        }
                    }
                    IconButton(onClick = onSave, enabled = actionsEnabled) {
                        Icon(
                            painterResource(R.drawable.ic_save),
                            contentDescription = stringResource(R.string.save),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }

            uiState.itemNotFound -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.item_not_found))
                TextButton(onClick = onCancel) { Text(stringResource(R.string.back)) }
            }

            else -> ItemFormTable(
                fields = uiState.fields,
                errors = uiState.errors,
                onNameChange = onNameChange,
                onMileageIntervalChange = onMileageIntervalChange,
                onRecurrenceValueChange = onRecurrenceValueChange,
                onRecurrenceUnitChange = onRecurrenceUnitChange,
                onReminderValueChange = onReminderValueChange,
                onReminderUnitChange = onReminderUnitChange,
                onLastDoneDateChange = onLastDoneDateChange,
                onLastDoneMileageChange = onLastDoneMileageChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            )
        }
    }

    if (confirmingDeletion) {
        DeleteItemDialog(
            itemName = uiState.fields.name,
            onConfirm = {
                confirmingDeletion = false
                onDelete()
            },
            onDismiss = { confirmingDeletion = false },
        )
    }
}

@Composable
private fun ItemFormTable(
    fields: MaintenanceItemFormFields,
    errors: MaintenanceItemFormErrors,
    onNameChange: (String) -> Unit,
    onMileageIntervalChange: (String) -> Unit,
    onRecurrenceValueChange: (String) -> Unit,
    onRecurrenceUnitChange: (IntervalUnit?) -> Unit,
    onReminderValueChange: (String) -> Unit,
    onReminderUnitChange: (IntervalUnit?) -> Unit,
    onLastDoneDateChange: (LocalDate?) -> Unit,
    onLastDoneMileageChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val service = stringResource(R.string.item_name)
    val dueEvery = stringResource(R.string.item_due_every)
    val remindEvery = stringResource(R.string.item_reminder_value)
    val lastDoneDate = stringResource(R.string.item_last_done_date)
    val lastDoneMileage = stringResource(R.string.item_last_done_mileage)
    val miles = stringResource(R.string.item_miles)
    var choosingDate by remember { mutableStateOf(false) }

    FormTable(
        modifier = modifier,
        labels = {
            listOf(service, dueEvery, dueEvery, remindEvery, lastDoneDate, lastDoneMileage)
                .forEach { FormCellText(it) }
        },
        inputs = {
            FormTextField(
                value = fields.name,
                onValueChange = onNameChange,
                description = service,
                error = errors.name?.message(),
                placeholder = stringResource(R.string.item_name_placeholder),
                modifier = Modifier.spansExtraColumn(),
            )
            FormTextField(
                value = fields.mileageInterval,
                onValueChange = onMileageIntervalChange,
                description = dueEvery,
                error = errors.mileageInterval?.message(),
                keyboardType = KeyboardType.Number,
            )
            FormTextField(
                value = fields.recurrenceValue,
                onValueChange = onRecurrenceValueChange,
                description = dueEvery,
                error = errors.recurrenceValue?.message(),
                keyboardType = KeyboardType.Number,
            )
            FormTextField(
                value = fields.reminderValue,
                onValueChange = onReminderValueChange,
                description = remindEvery,
                error = errors.reminderValue?.message(),
                keyboardType = KeyboardType.Number,
            )
            ChooseDateButton(
                onClick = { choosingDate = true },
                error = errors.lastDoneDate?.message(),
            )
            FormTextField(
                value = fields.lastDoneMileage,
                onValueChange = onLastDoneMileageChange,
                description = lastDoneMileage,
                error = errors.lastDoneMileage?.message(),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            )
        },
        extras = {
            Spacer(Modifier)
            FormCellText(miles)
            UnitDropdown(
                unit = fields.recurrenceUnit,
                onUnitChange = onRecurrenceUnitChange,
                description = stringResource(R.string.item_unit_description, dueEvery),
                error = errors.recurrenceUnit?.message(),
                allowNoUnit = true,
            )
            UnitDropdown(
                unit = fields.reminderUnit,
                onUnitChange = onReminderUnitChange,
                description = stringResource(R.string.item_unit_description, remindEvery),
                error = errors.reminderUnit?.message(),
                allowNoUnit = false,
            )
            FormCellText(
                fields.lastDoneDate?.let { formatShortDate(it) }
                    ?: stringResource(R.string.item_date_not_set),
            )
            FormCellText(miles)
        },
    )

    if (choosingDate) {
        FormDatePickerDialog(
            date = fields.lastDoneDate,
            onDateChange = onLastDoneDateChange,
            onDismiss = { choosingDate = false },
            onClear = { onLastDoneDateChange(null) }.takeIf { fields.lastDoneDate != null },
        )
    }
}

private val UNIT_FIELD_WIDTH = 132.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    unit: IntervalUnit?,
    onUnitChange: (IntervalUnit?) -> Unit,
    description: String,
    error: String?,
    allowNoUnit: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = if (allowNoUnit) listOf(null) + IntervalUnit.entries else IntervalUnit.entries

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = unitText(unit),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            isError = error != null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .width(UNIT_FIELD_WIDTH)
                .describedAs(description, error),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(unitText(option)) },
                    onClick = {
                        onUnitChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun unitText(unit: IntervalUnit?): String =
    unit?.let { unitLabel(it) } ?: stringResource(R.string.item_date_not_set)

@Composable
private fun ItemFieldError.message(): String = stringResource(
    when (this) {
        ItemFieldError.REQUIRED -> R.string.error_required
        ItemFieldError.NOT_A_POSITIVE_NUMBER -> R.string.error_positive_number
        ItemFieldError.NOT_A_NON_NEGATIVE_NUMBER -> R.string.error_non_negative_number
        ItemFieldError.UNIT_REQUIRED -> R.string.error_unit_required
        ItemFieldError.DATE_IN_FUTURE -> R.string.error_date_in_future
    },
)

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MaintenanceItemFormPreview() {
    VehicleMaintenanceTheme {
        MaintenanceItemFormContent(
            uiState = MaintenanceItemFormUiState(
                isLoading = false,
                fields = MaintenanceItemFormFields(
                    name = "Oil change",
                    mileageInterval = "5000",
                    recurrenceValue = "6",
                    recurrenceUnit = IntervalUnit.MONTHS,
                    reminderValue = "5",
                    reminderUnit = IntervalUnit.MONTHS,
                    lastDoneDate = LocalDate.of(2026, 3, 15),
                    lastDoneMileage = "42000",
                ),
            ),
            onNameChange = {},
            onMileageIntervalChange = {},
            onRecurrenceValueChange = {},
            onRecurrenceUnitChange = {},
            onReminderValueChange = {},
            onReminderUnitChange = {},
            onLastDoneDateChange = {},
            onLastDoneMileageChange = {},
            onSave = {},
            onDelete = {},
            onCancel = {},
            onSaveErrorShown = {},
            onDeleteErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MaintenanceItemFormErrorsPreview() {
    VehicleMaintenanceTheme {
        MaintenanceItemFormContent(
            uiState = MaintenanceItemFormUiState(
                isLoading = false,
                fields = MaintenanceItemFormFields(recurrenceValue = "6", recurrenceUnit = null),
                errors = MaintenanceItemFormErrors(
                    name = ItemFieldError.REQUIRED,
                    recurrenceUnit = ItemFieldError.UNIT_REQUIRED,
                    reminderValue = ItemFieldError.REQUIRED,
                ),
            ),
            onNameChange = {},
            onMileageIntervalChange = {},
            onRecurrenceValueChange = {},
            onRecurrenceUnitChange = {},
            onReminderValueChange = {},
            onReminderUnitChange = {},
            onLastDoneDateChange = {},
            onLastDoneMileageChange = {},
            onSave = {},
            onDelete = {},
            onCancel = {},
            onSaveErrorShown = {},
            onDeleteErrorShown = {},
        )
    }
}

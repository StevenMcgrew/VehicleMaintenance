package com.example.vehiclemaintenance.maintenance

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import kotlinx.coroutines.flow.drop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

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
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                },
                actions = {
                    if (uiState.isEditing && !uiState.itemNotFound) {
                        TextButton(
                            onClick = { confirmingDeletion = true },
                            enabled = actionsEnabled,
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                    TextButton(onClick = onSave, enabled = actionsEnabled) {
                        Text(stringResource(R.string.save))
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

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ItemTextField(
                    value = uiState.fields.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.item_name),
                    placeholder = stringResource(R.string.item_name_placeholder),
                    error = uiState.errors.name?.message(),
                )
                ItemTextField(
                    value = uiState.fields.mileageInterval,
                    onValueChange = onMileageIntervalChange,
                    label = stringResource(R.string.item_mileage_interval),
                    placeholder = stringResource(R.string.item_mileage_interval_placeholder),
                    error = uiState.errors.mileageInterval?.message(),
                    numeric = true,
                )
                IntervalFields(
                    label = stringResource(R.string.item_recurrence_value),
                    placeholder = stringResource(R.string.item_recurrence_value_placeholder),
                    value = uiState.fields.recurrenceValue,
                    onValueChange = onRecurrenceValueChange,
                    unit = uiState.fields.recurrenceUnit,
                    onUnitChange = onRecurrenceUnitChange,
                    valueError = uiState.errors.recurrenceValue?.message(),
                    unitError = uiState.errors.recurrenceUnit?.message(),
                    allowNoUnit = true,
                )
                IntervalFields(
                    label = stringResource(R.string.item_reminder_value),
                    placeholder = stringResource(R.string.item_reminder_value_placeholder),
                    value = uiState.fields.reminderValue,
                    onValueChange = onReminderValueChange,
                    unit = uiState.fields.reminderUnit,
                    onUnitChange = onReminderUnitChange,
                    valueError = uiState.errors.reminderValue?.message(),
                    unitError = uiState.errors.reminderUnit?.message(),
                    allowNoUnit = false,
                )
                LastDoneDateField(
                    date = uiState.fields.lastDoneDate,
                    onDateChange = onLastDoneDateChange,
                    error = uiState.errors.lastDoneDate?.message(),
                )
                ItemTextField(
                    value = uiState.fields.lastDoneMileage,
                    onValueChange = onLastDoneMileageChange,
                    label = stringResource(R.string.item_last_done_mileage),
                    placeholder = stringResource(R.string.item_last_done_mileage_placeholder),
                    error = uiState.errors.lastDoneMileage?.message(),
                    numeric = true,
                    imeAction = ImeAction.Done,
                )
            }
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
private fun ItemTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    numeric: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        state = rememberEditedFieldState(value, onValueChange),
        labelPosition = MINIMIZED_LABEL,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = error != null,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = imeAction,
        ),
        supportingText = error?.let { { Text(it) } },
        modifier = modifier
            .fillMaxWidth()
            .then(if (error != null) Modifier.semantics { error(error) } else Modifier),
    )
}

/** Pinning a label into the outline needs the [TextFieldState] overload, so bridge to it. */
@Composable
private fun rememberEditedFieldState(
    value: String,
    onValueChange: (String) -> Unit,
): TextFieldState {
    val state = rememberTextFieldState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.drop(1).collect(currentOnValueChange)
    }
    return state
}

@Composable
private fun rememberShownFieldState(shown: String): TextFieldState {
    val state = rememberTextFieldState(shown)
    LaunchedEffect(shown) { state.setTextAndPlaceCursorAtEnd(shown) }
    return state
}

private val MINIMIZED_LABEL = TextFieldLabelPosition.Attached(alwaysMinimize = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalFields(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: IntervalUnit?,
    onUnitChange: (IntervalUnit?) -> Unit,
    valueError: String?,
    unitError: String?,
    allowNoUnit: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val unitFieldLabel = stringResource(R.string.item_unit)
    val unitPlaceholder = stringResource(R.string.item_unit_placeholder)
    val selected = unit?.let { unitLabel(it) }.orEmpty()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            state = rememberEditedFieldState(value, onValueChange),
            labelPosition = MINIMIZED_LABEL,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            isError = valueError != null,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            supportingText = valueError?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (valueError != null) Modifier.semantics { error(valueError) } else Modifier,
                ),
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                state = rememberShownFieldState(selected),
                readOnly = true,
                labelPosition = MINIMIZED_LABEL,
                label = { Text(unitFieldLabel) },
                placeholder = { Text(unitPlaceholder) },
                isError = unitError != null,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                supportingText = unitError?.let { { Text(it) } },
                modifier = Modifier
                    .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .then(
                        if (unitError != null) Modifier.semantics { error(unitError) } else Modifier,
                    ),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (allowNoUnit) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.item_date_not_set)) },
                        onClick = {
                            onUnitChange(null)
                            expanded = false
                        },
                    )
                }
                IntervalUnit.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(unitLabel(option)) },
                        onClick = {
                            onUnitChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastDoneDateField(
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val label = stringResource(R.string.item_last_done_date)
    val notSet = stringResource(R.string.item_date_not_set)
    val shown = date?.toString().orEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            state = rememberShownFieldState(shown),
            readOnly = true,
            labelPosition = MINIMIZED_LABEL,
            label = { Text(label) },
            placeholder = { Text(notSet) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (error != null) Modifier.semantics { error(error) } else Modifier),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showPicker = true }) { Text(label) }
            if (date != null) {
                TextButton(onClick = { onDateChange(null) }) {
                    Text(stringResource(R.string.item_clear_date))
                }
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date?.toEpochDay()?.times(MILLIS_PER_DAY),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateChange(state.selectedDateMillis?.toLocalDate())
                        showPicker = false
                    },
                ) {
                    Text(stringResource(R.string.item_confirm_date))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private const val MILLIS_PER_DAY = 86_400_000L

/** The picker reports UTC midnight, so read it back in UTC or the day can shift. */
private fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

@Composable
private fun ItemFieldError.message(): String = stringResource(
    when (this) {
        ItemFieldError.REQUIRED -> R.string.error_required
        ItemFieldError.NOT_A_POSITIVE_NUMBER -> R.string.error_positive_number
        ItemFieldError.NOT_A_NON_NEGATIVE_NUMBER -> R.string.error_non_negative_number
        ItemFieldError.VALUE_REQUIRED -> R.string.error_value_required
        ItemFieldError.UNIT_REQUIRED -> R.string.error_unit_required
        ItemFieldError.DATE_IN_FUTURE -> R.string.error_date_in_future
    },
)

@Preview(showBackground = true)
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
                errors = MaintenanceItemFormErrors(
                    name = ItemFieldError.REQUIRED,
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

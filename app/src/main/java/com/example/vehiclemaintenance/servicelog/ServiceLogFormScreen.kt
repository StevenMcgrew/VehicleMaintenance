package com.example.vehiclemaintenance.servicelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import kotlinx.coroutines.flow.drop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ServiceLogFormScreen(
    vehicleId: String,
    itemId: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceLogFormViewModel = viewModel(
        key = "log-$itemId",
        factory = ServiceLogFormViewModel.factory(vehicleId, itemId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onDone()
    }

    ServiceLogFormContent(
        uiState = uiState,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDateChange = viewModel::onDateChange,
        onOdometerChange = viewModel::onOdometerChange,
        onCostChange = viewModel::onCostChange,
        onNotesChange = viewModel::onNotesChange,
        onSave = viewModel::save,
        onCancel = onDone,
        onSaveErrorShown = viewModel::dismissSaveError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceLogFormContent(
    uiState: ServiceLogFormUiState,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onOdometerChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.log_save_failed)

    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) {
            snackbarHostState.showSnackbar(saveFailedMessage)
            onSaveErrorShown()
        }
    }

    val actionsEnabled = !uiState.isLoading && !uiState.isSaving && !uiState.itemNotFound

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_service_title)) },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                },
                actions = {
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
                LogTextField(
                    value = uiState.fields.description,
                    onValueChange = onDescriptionChange,
                    label = stringResource(R.string.log_description),
                    placeholder = stringResource(R.string.log_description_placeholder),
                    error = uiState.errors.description?.message(),
                )
                LogDateField(
                    date = uiState.fields.date,
                    onDateChange = onDateChange,
                    error = uiState.errors.date?.message(),
                )
                LogTextField(
                    value = uiState.fields.odometer,
                    onValueChange = onOdometerChange,
                    label = stringResource(R.string.log_odometer),
                    placeholder = stringResource(R.string.log_odometer_placeholder),
                    error = uiState.errors.odometer?.message(),
                    keyboardType = KeyboardType.Number,
                )
                LogTextField(
                    value = uiState.fields.cost,
                    onValueChange = onCostChange,
                    label = stringResource(R.string.log_cost),
                    placeholder = stringResource(R.string.log_cost_placeholder),
                    error = uiState.errors.cost?.message(),
                    keyboardType = KeyboardType.Decimal,
                )
                LogTextField(
                    value = uiState.fields.notes,
                    onValueChange = onNotesChange,
                    label = stringResource(R.string.log_notes),
                    placeholder = stringResource(R.string.log_notes_placeholder),
                    error = null,
                    imeAction = ImeAction.Done,
                )
            }
        }
    }
}

@Composable
private fun LogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        state = rememberEditedFieldState(value, onValueChange),
        labelPosition = MINIMIZED_LABEL,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = error != null,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
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
private fun LogDateField(
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    val label = stringResource(R.string.log_date)
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
        TextButton(onClick = { showPicker = true }) {
            Text(stringResource(R.string.log_choose_date))
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
private fun LogFieldError.message(): String = stringResource(
    when (this) {
        LogFieldError.REQUIRED -> R.string.error_required
        LogFieldError.NOT_A_NON_NEGATIVE_NUMBER -> R.string.error_non_negative_number
        LogFieldError.NOT_A_VALID_AMOUNT -> R.string.error_valid_amount
        LogFieldError.DATE_IN_FUTURE -> R.string.error_date_in_future
    },
)

@Preview(showBackground = true)
@Composable
private fun ServiceLogFormPreview() {
    VehicleMaintenanceTheme {
        ServiceLogFormContent(
            uiState = ServiceLogFormUiState(
                isLoading = false,
                fields = ServiceLogFormFields(
                    description = "Oil change",
                    date = LocalDate.of(2026, 9, 5),
                    odometer = "48000",
                    cost = "64.99",
                    notes = "Shop said the belts look fine",
                ),
            ),
            onDescriptionChange = {},
            onDateChange = {},
            onOdometerChange = {},
            onCostChange = {},
            onNotesChange = {},
            onSave = {},
            onCancel = {},
            onSaveErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceLogFormErrorsPreview() {
    VehicleMaintenanceTheme {
        ServiceLogFormContent(
            uiState = ServiceLogFormUiState(
                isLoading = false,
                fields = ServiceLogFormFields(
                    description = "",
                    date = LocalDate.of(2026, 12, 25),
                    odometer = "",
                    cost = "45.555",
                ),
                errors = ServiceLogFormErrors(
                    description = LogFieldError.REQUIRED,
                    date = LogFieldError.DATE_IN_FUTURE,
                    odometer = LogFieldError.REQUIRED,
                    cost = LogFieldError.NOT_A_VALID_AMOUNT,
                ),
            ),
            onDescriptionChange = {},
            onDateChange = {},
            onOdometerChange = {},
            onCostChange = {},
            onNotesChange = {},
            onSave = {},
            onCancel = {},
            onSaveErrorShown = {},
        )
    }
}

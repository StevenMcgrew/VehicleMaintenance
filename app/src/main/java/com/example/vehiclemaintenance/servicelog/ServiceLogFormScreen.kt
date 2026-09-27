package com.example.vehiclemaintenance.servicelog

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.maintenance.formatShortDate
import com.example.vehiclemaintenance.ui.ChooseDateButton
import com.example.vehiclemaintenance.ui.FormCellText
import com.example.vehiclemaintenance.ui.FormDatePickerDialog
import com.example.vehiclemaintenance.ui.FormTable
import com.example.vehiclemaintenance.ui.FormTextField
import com.example.vehiclemaintenance.ui.brandIconButtonColors
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.ui.spansExtraColumn
import java.time.LocalDate

@Composable
fun ServiceLogFormScreen(
    vehicleId: String,
    /** Null logs an ad-hoc repair instead of completing a tracked item. */
    itemId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceLogFormViewModel = viewModel(
        key = "log-${itemId ?: "repair"}",
        factory = ServiceLogFormViewModel.factory(vehicleId, itemId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onDone()
    }

    ServiceLogFormContent(
        uiState = uiState,
        isAdHocRepair = itemId == null,
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
    isAdHocRepair: Boolean,
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

    val actionsEnabled = !uiState.isLoading && !uiState.isSaving &&
        !uiState.itemNotFound && !uiState.vehicleNotFound

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isAdHocRepair) R.string.log_repair_title
                            else R.string.log_service_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, colors = brandIconButtonColors()) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSave,
                        enabled = actionsEnabled,
                        colors = brandIconButtonColors(),
                    ) {
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

            uiState.itemNotFound || uiState.vehicleNotFound -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(
                        if (uiState.vehicleNotFound) R.string.vehicle_not_found
                        else R.string.item_not_found,
                    ),
                )
                TextButton(onClick = onCancel) { Text(stringResource(R.string.back)) }
            }

            else -> LogFormTable(
                fields = uiState.fields,
                errors = uiState.errors,
                isAdHocRepair = isAdHocRepair,
                onDescriptionChange = onDescriptionChange,
                onDateChange = onDateChange,
                onOdometerChange = onOdometerChange,
                onCostChange = onCostChange,
                onNotesChange = onNotesChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun LogFormTable(
    fields: ServiceLogFormFields,
    errors: ServiceLogFormErrors,
    isAdHocRepair: Boolean,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (LocalDate?) -> Unit,
    onOdometerChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(
        if (isAdHocRepair) R.string.log_repair_label else R.string.log_service_label,
    )
    val date = stringResource(R.string.log_date)
    val odometer = stringResource(R.string.log_odometer)
    val cost = stringResource(R.string.log_cost)
    val notes = stringResource(R.string.log_notes)
    val optional = stringResource(R.string.optional_marker)
    var choosingDate by remember { mutableStateOf(false) }

    FormTable(
        modifier = modifier,
        labels = {
            listOf(description, date, odometer, cost, notes).forEach { FormCellText(it) }
        },
        inputs = {
            FormTextField(
                value = fields.description,
                onValueChange = onDescriptionChange,
                description = description,
                error = errors.description?.message(),
                placeholder = stringResource(
                    if (isAdHocRepair) R.string.log_repair_description_placeholder
                    else R.string.log_description_placeholder,
                ),
                modifier = Modifier.spansExtraColumn(),
            )
            ChooseDateButton(
                onClick = { choosingDate = true },
                error = errors.date?.message(),
            )
            FormTextField(
                value = fields.odometer,
                onValueChange = onOdometerChange,
                description = odometer,
                error = errors.odometer?.message(),
                keyboardType = KeyboardType.Number,
            )
            FormTextField(
                value = fields.cost,
                onValueChange = onCostChange,
                description = cost,
                error = errors.cost?.message(),
                keyboardType = KeyboardType.Decimal,
            )
            FormTextField(
                value = fields.notes,
                onValueChange = onNotesChange,
                description = notes,
                error = null,
                imeAction = ImeAction.Done,
            )
        },
        extras = {
            Spacer(Modifier)
            FormCellText(
                fields.date?.let { formatShortDate(it) }
                    ?: stringResource(R.string.item_date_not_set),
            )
            FormCellText(stringResource(R.string.item_miles))
            FormCellText(optional)
            FormCellText(optional)
        },
    )

    if (choosingDate) {
        FormDatePickerDialog(
            date = fields.date,
            onDateChange = onDateChange,
            onDismiss = { choosingDate = false },
        )
    }
}

@Composable
private fun LogFieldError.message(): String = stringResource(
    when (this) {
        LogFieldError.REQUIRED -> R.string.error_required
        LogFieldError.NOT_A_NON_NEGATIVE_NUMBER -> R.string.error_non_negative_number
        LogFieldError.NOT_A_VALID_AMOUNT -> R.string.error_valid_amount
        LogFieldError.DATE_IN_FUTURE -> R.string.error_date_in_future
    },
)

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
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
            isAdHocRepair = false,
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
            isAdHocRepair = false,
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

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ServiceLogRepairPreview() {
    VehicleMaintenanceTheme {
        ServiceLogFormContent(
            uiState = ServiceLogFormUiState(
                isLoading = false,
                fields = ServiceLogFormFields(date = LocalDate.of(2026, 9, 5)),
            ),
            isAdHocRepair = true,
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

package com.example.vehiclemaintenance.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

@Composable
fun VehicleFormScreen(
    vehicleId: String?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleFormViewModel = viewModel(
        key = vehicleId ?: "new-vehicle",
        factory = VehicleFormViewModel.factory(vehicleId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onDone()
    }

    VehicleFormContent(
        uiState = uiState,
        onYearChange = viewModel::onYearChange,
        onMakeChange = viewModel::onMakeChange,
        onModelChange = viewModel::onModelChange,
        onEngineChange = viewModel::onEngineChange,
        onSave = viewModel::save,
        onCancel = onDone,
        onSaveErrorShown = viewModel::dismissSaveError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormContent(
    uiState: VehicleFormUiState,
    onYearChange: (String) -> Unit,
    onMakeChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onEngineChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.vehicle_save_failed)

    LaunchedEffect(uiState.saveFailed) {
        if (uiState.saveFailed) {
            snackbarHostState.showSnackbar(saveFailedMessage)
            onSaveErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.edit_vehicle else R.string.add_vehicle,
                        ),
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                },
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = !uiState.isLoading &&
                            !uiState.isSaving &&
                            !uiState.vehicleNotFound,
                    ) {
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

            uiState.vehicleNotFound -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.vehicle_not_found))
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
                VehicleField(
                    value = uiState.fields.year,
                    onValueChange = onYearChange,
                    label = stringResource(R.string.vehicle_year),
                    error = uiState.errors.year?.message(uiState.minYear, uiState.maxYear),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
                VehicleField(
                    value = uiState.fields.make,
                    onValueChange = onMakeChange,
                    label = stringResource(R.string.vehicle_make),
                    error = uiState.errors.make?.message(uiState.minYear, uiState.maxYear),
                )
                VehicleField(
                    value = uiState.fields.model,
                    onValueChange = onModelChange,
                    label = stringResource(R.string.vehicle_model),
                    error = uiState.errors.model?.message(uiState.minYear, uiState.maxYear),
                )
                VehicleField(
                    value = uiState.fields.engine,
                    onValueChange = onEngineChange,
                    label = stringResource(R.string.vehicle_engine),
                    error = uiState.errors.engine?.message(uiState.minYear, uiState.maxYear),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
            }
        }
    }
}

@Composable
private fun VehicleField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        supportingText = error?.let { { Text(it) } },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (error != null) Modifier.semantics { error(error) } else Modifier,
            ),
    )
}

@Composable
private fun VehicleFieldError.message(minYear: Int, maxYear: Int): String = when (this) {
    VehicleFieldError.REQUIRED -> stringResource(R.string.error_required)
    VehicleFieldError.YEAR_NOT_A_NUMBER -> stringResource(R.string.error_year_not_a_number)
    VehicleFieldError.YEAR_OUT_OF_RANGE ->
        stringResource(R.string.error_year_out_of_range, minYear, maxYear)
}

@Preview(showBackground = true)
@Composable
private fun VehicleFormPreview() {
    VehicleMaintenanceTheme {
        VehicleFormContent(
            uiState = VehicleFormUiState(
                isLoading = false,
                maxYear = 2027,
                fields = VehicleFormFields(
                    year = "2014",
                    make = "Toyota",
                    model = "Tacoma",
                    engine = "4.0L V6",
                ),
            ),
            onYearChange = {},
            onMakeChange = {},
            onModelChange = {},
            onEngineChange = {},
            onSave = {},
            onCancel = {},
            onSaveErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleFormErrorsPreview() {
    VehicleMaintenanceTheme {
        VehicleFormContent(
            uiState = VehicleFormUiState(
                isLoading = false,
                maxYear = 2027,
                errors = VehicleFormErrors(
                    year = VehicleFieldError.YEAR_OUT_OF_RANGE,
                    make = VehicleFieldError.REQUIRED,
                    model = VehicleFieldError.REQUIRED,
                    engine = VehicleFieldError.REQUIRED,
                ),
                fields = VehicleFormFields(year = "1800"),
            ),
            onYearChange = {},
            onMakeChange = {},
            onModelChange = {},
            onEngineChange = {},
            onSave = {},
            onCancel = {},
            onSaveErrorShown = {},
        )
    }
}

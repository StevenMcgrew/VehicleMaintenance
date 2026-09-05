package com.example.vehiclemaintenance.vehicles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

@Composable
fun VehicleListScreen(
    onAddVehicle: () -> Unit,
    onEditVehicle: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleListViewModel = viewModel(factory = VehicleListViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    VehicleListContent(
        uiState = uiState,
        onAddVehicle = onAddVehicle,
        onEditVehicle = onEditVehicle,
        onDeleteVehicle = viewModel::deleteVehicle,
        onRetry = viewModel::refresh,
        onDeleteErrorShown = viewModel::dismissDeleteError,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListContent(
    uiState: VehicleListUiState,
    onAddVehicle: () -> Unit,
    onEditVehicle: (String) -> Unit,
    onDeleteVehicle: (String) -> Unit,
    onRetry: () -> Unit,
    onDeleteErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.delete_vehicle_failed)

    LaunchedEffect(uiState.deleteFailed) {
        if (uiState.deleteFailed) {
            snackbarHostState.showSnackbar(deleteFailedMessage)
            onDeleteErrorShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.vehicles_title)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.loadFailed) {
                ExtendedFloatingActionButton(
                    onClick = onAddVehicle,
                    text = { Text(stringResource(R.string.add_vehicle)) },
                    icon = {},
                )
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> CenteredColumn(Modifier.padding(innerPadding)) {
                CircularProgressIndicator()
            }

            uiState.loadFailed -> CenteredColumn(Modifier.padding(innerPadding)) {
                Text(
                    text = stringResource(R.string.vehicles_load_error),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }

            uiState.vehicles.isEmpty() -> CenteredColumn(Modifier.padding(innerPadding)) {
                Text(
                    text = stringResource(R.string.vehicles_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.vehicles_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(uiState.vehicles, key = { it.id }) { vehicle ->
                    VehicleRow(
                        vehicle = vehicle,
                        onEdit = { onEditVehicle(vehicle.id) },
                        onDelete = { pendingDeletionId = vehicle.id },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val pendingVehicle = uiState.vehicles.firstOrNull { it.id == pendingDeletionId }
    if (pendingVehicle != null) {
        DeleteVehicleDialog(
            vehicleLabel = pendingVehicle.primaryLabel(),
            onConfirm = {
                onDeleteVehicle(pendingVehicle.id)
                pendingDeletionId = null
            },
            onDismiss = { pendingDeletionId = null },
        )
    }
}

@Composable
private fun VehicleRow(
    vehicle: Vehicle,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = vehicle.primaryLabel()
    val deleteLabel = stringResource(R.string.delete_vehicle_action, label)
    ListItem(
        modifier = modifier.clickable(onClick = onEdit),
        headlineContent = {
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                text = vehicle.secondaryLabel(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = deleteLabel
                },
            ) {
                Text(stringResource(R.string.delete))
            }
        },
    )
}

@Composable
private fun DeleteVehicleDialog(
    vehicleLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_vehicle_title, vehicleLabel)) },
        text = { Text(stringResource(R.string.delete_vehicle_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun CenteredColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Composable
private fun Vehicle.primaryLabel(): String =
    nickname ?: stringResource(R.string.vehicle_summary, year, make, model)

@Composable
private fun Vehicle.secondaryLabel(): String = if (nickname == null) {
    engine
} else {
    "${stringResource(R.string.vehicle_summary, year, make, model)} - $engine"
}

@Preview(showBackground = true)
@Composable
private fun VehicleListEmptyPreview() {
    VehicleMaintenanceTheme {
        VehicleListContent(
            uiState = VehicleListUiState(isLoading = false),
            onAddVehicle = {},
            onEditVehicle = {},
            onDeleteVehicle = {},
            onRetry = {},
            onDeleteErrorShown = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleListPreview() {
    VehicleMaintenanceTheme {
        VehicleListContent(
            uiState = VehicleListUiState(
                isLoading = false,
                vehicles = listOf(
                    Vehicle("1", "Daily", 2014, "Toyota", "Tacoma", "4.0L V6"),
                    Vehicle("2", null, 2020, "Honda", "Civic", "2.0L I4"),
                ),
            ),
            onAddVehicle = {},
            onEditVehicle = {},
            onDeleteVehicle = {},
            onRetry = {},
            onDeleteErrorShown = {},
        )
    }
}

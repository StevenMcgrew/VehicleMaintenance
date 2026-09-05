package com.example.vehiclemaintenance.maintenance

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
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
import com.example.vehiclemaintenance.vehicles.Vehicle

@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    onEditVehicle: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VehicleDetailViewModel = viewModel(
        key = vehicleId,
        factory = VehicleDetailViewModel.factory(vehicleId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    VehicleDetailContent(
        uiState = uiState,
        onEditVehicle = onEditVehicle,
        onAddItem = onAddItem,
        onEditItem = onEditItem,
        onDeleteItem = viewModel::deleteItem,
        onRetry = viewModel::refresh,
        onDeleteErrorShown = viewModel::dismissDeleteError,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailContent(
    uiState: VehicleDetailUiState,
    onEditVehicle: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onRetry: () -> Unit,
    onDeleteErrorShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.delete_item_failed)

    LaunchedEffect(uiState.deleteFailed) {
        if (uiState.deleteFailed) {
            snackbarHostState.showSnackbar(deleteFailedMessage)
            onDeleteErrorShown()
        }
    }

    val vehicle = uiState.vehicle
    val title = vehicle?.let {
        stringResource(R.string.vehicle_summary, it.year, it.make, it.model)
    } ?: stringResource(R.string.maintenance_title)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
                actions = {
                    if (vehicle != null) {
                        TextButton(onClick = onEditVehicle) {
                            Text(stringResource(R.string.edit_vehicle))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!uiState.loadFailed && !uiState.vehicleNotFound && vehicle != null) {
                ExtendedFloatingActionButton(onClick = onAddItem) {
                    Text(stringResource(R.string.add_maintenance_item))
                }
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

            uiState.vehicleNotFound || vehicle == null -> CenteredColumn(
                Modifier.padding(innerPadding),
            ) {
                Text(stringResource(R.string.vehicle_not_found))
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }

            uiState.items.isEmpty() -> CenteredColumn(Modifier.padding(innerPadding)) {
                Text(
                    text = stringResource(R.string.maintenance_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.maintenance_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(modifier = Modifier.padding(innerPadding)) {
                items(uiState.items, key = { it.id }) { item ->
                    MaintenanceItemRow(
                        item = item,
                        onEdit = { onEditItem(item.id) },
                        onDelete = { pendingDeletionId = item.id },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val pendingItem = uiState.items.firstOrNull { it.id == pendingDeletionId }
    if (pendingItem != null) {
        DeleteItemDialog(
            itemName = pendingItem.name,
            onConfirm = {
                onDeleteItem(pendingItem.id)
                pendingDeletionId = null
            },
            onDismiss = { pendingDeletionId = null },
        )
    }
}

@Composable
private fun MaintenanceItemRow(
    item: MaintenanceItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteLabel = stringResource(R.string.delete_item_action, item.name)
    ListItem(
        modifier = modifier.clickable(onClick = onEdit),
        headlineContent = {
            Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = item.scheduleSummary(), maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            TextButton(
                onClick = onDelete,
                modifier = Modifier.clearAndSetSemantics { contentDescription = deleteLabel },
            ) {
                Text(stringResource(R.string.delete))
            }
        },
    )
}

/** Only what the user actually set, in a fixed order. Due status arrives with feature 6. */
@Composable
private fun MaintenanceItem.scheduleSummary(): String {
    val parts = buildList {
        mileageInterval?.let {
            add(stringResource(R.string.item_every_interval, pluralStringResource(R.plurals.item_miles, it, it)))
        }
        recurrence?.let { add(stringResource(R.string.item_every_interval, it.spelled())) }
        add(stringResource(R.string.item_remind_after, reminder.spelled()))
    }
    return parts.joinToString(stringResource(R.string.item_summary_separator))
}

/** "5 months", pluralized, so a value of 1 does not read as "1 months". */
@Composable
private fun Interval.spelled(): String = pluralStringResource(
    when (unit) {
        IntervalUnit.DAYS -> R.plurals.duration_days
        IntervalUnit.WEEKS -> R.plurals.duration_weeks
        IntervalUnit.MONTHS -> R.plurals.duration_months
        IntervalUnit.YEARS -> R.plurals.duration_years
    },
    value,
    value,
)

@Composable
fun unitLabel(unit: IntervalUnit): String = stringResource(
    when (unit) {
        IntervalUnit.DAYS -> R.string.unit_days
        IntervalUnit.WEEKS -> R.string.unit_weeks
        IntervalUnit.MONTHS -> R.string.unit_months
        IntervalUnit.YEARS -> R.string.unit_years
    },
)

@Composable
private fun DeleteItemDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_item_title, itemName)) },
        text = { Text(stringResource(R.string.delete_item_message)) },
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

private val previewVehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")

@Preview(showBackground = true)
@Composable
private fun VehicleDetailEmptyPreview() {
    VehicleMaintenanceTheme {
        VehicleDetailContent(
            uiState = VehicleDetailUiState(isLoading = false, vehicle = previewVehicle),
            onEditVehicle = {},
            onAddItem = {},
            onEditItem = {},
            onDeleteItem = {},
            onRetry = {},
            onDeleteErrorShown = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleDetailPreview() {
    VehicleMaintenanceTheme {
        VehicleDetailContent(
            uiState = VehicleDetailUiState(
                isLoading = false,
                vehicle = previewVehicle,
                items = listOf(
                    MaintenanceItem(
                        id = "m-1",
                        vehicleId = "v-1",
                        name = "Oil change",
                        mileageInterval = 5000,
                        recurrence = Interval(6, IntervalUnit.MONTHS),
                        reminder = Interval(5, IntervalUnit.MONTHS),
                    ),
                    MaintenanceItem(
                        id = "m-2",
                        vehicleId = "v-1",
                        name = "Tire rotation",
                        mileageInterval = 7500,
                        reminder = Interval(90, IntervalUnit.DAYS),
                    ),
                ),
            ),
            onEditVehicle = {},
            onAddItem = {},
            onEditItem = {},
            onDeleteItem = {},
            onRetry = {},
            onDeleteErrorShown = {},
            onBack = {},
        )
    }
}

package com.example.vehiclemaintenance.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.stringResource
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
    onLogService: (String) -> Unit,
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
        onLogService = onLogService,
        onDeleteItem = viewModel::deleteItem,
        onDeleteErrorShown = viewModel::dismissDeleteError,
        onRetry = viewModel::refresh,
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
    onLogService: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteErrorShown: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vehicle = uiState.vehicle
    // The sheet and the dialog track an id, not the item, so a concurrent edit cannot show stale
    // text; the name is read back out of the current list each recomposition.
    var actionsForItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmingDeletionOfItemId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = stringResource(R.string.delete_item_failed)

    LaunchedEffect(uiState.deleteFailed) {
        if (uiState.deleteFailed) {
            snackbarHostState.showSnackbar(deleteFailedMessage)
            onDeleteErrorShown()
        }
    }
    val title = vehicle?.let {
        stringResource(R.string.vehicle_summary_with_engine, it.year, it.make, it.model, it.engine)
    } ?: stringResource(R.string.maintenance_title)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

            else -> MaintenanceItemTable(
                items = uiState.items,
                onOpenItemActions = { actionsForItemId = it },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    val actionsItem = uiState.items.firstOrNull { it.id == actionsForItemId }
    if (actionsItem != null) {
        MaintenanceItemActionsSheet(
            itemName = actionsItem.name,
            onLogService = {
                actionsForItemId = null
                onLogService(actionsItem.id)
            },
            onEdit = {
                actionsForItemId = null
                onEditItem(actionsItem.id)
            },
            onDelete = {
                actionsForItemId = null
                confirmingDeletionOfItemId = actionsItem.id
            },
            onDismiss = { actionsForItemId = null },
        )
    }

    val deletingItem = uiState.items.firstOrNull { it.id == confirmingDeletionOfItemId }
    if (deletingItem != null) {
        DeleteItemDialog(
            itemName = deletingItem.name,
            onConfirm = {
                confirmingDeletionOfItemId = null
                onDeleteItem(deletingItem.id)
            },
            onDismiss = { confirmingDeletionOfItemId = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceItemActionsSheet(
    itemName: String,
    onLogService: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.item_actions_title, itemName),
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            SheetAction(stringResource(R.string.item_action_log_service), onLogService)
            SheetAction(stringResource(R.string.edit_maintenance_item), onEdit)
            SheetAction(stringResource(R.string.delete_maintenance_item), onDelete)
        }
    }
}

@Composable
private fun SheetAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 14.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun MaintenanceItemTable(
    items: List<MaintenanceItem>,
    onOpenItemActions: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MaintenanceItemHeader()
        HorizontalDivider()
        LazyColumn {
            items(items, key = { it.id }) { item ->
                MaintenanceItemRow(item = item, onClick = { onOpenItemActions(item.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun MaintenanceItemHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING),
    ) {
        HeaderCell(stringResource(R.string.column_service), SERVICE_WEIGHT, TextAlign.Start)
        HeaderCell(stringResource(R.string.column_miles), VALUE_WEIGHT, TextAlign.End)
        HeaderCell(stringResource(R.string.column_time), VALUE_WEIGHT, TextAlign.End)
        HeaderCell(stringResource(R.string.column_remind), VALUE_WEIGHT, TextAlign.End)
    }
}

@Composable
private fun RowScope.HeaderCell(text: String, weight: Float, textAlign: TextAlign) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MaintenanceItemRow(
    item: MaintenanceItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.name,
            modifier = Modifier.weight(SERVICE_WEIGHT),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ValueCell(item.mileageInterval?.let { formatMileage(it) })
        ValueCell(item.recurrence?.shortLabel())
        ValueCell(item.reminder.shortLabel())
    }
}

@Composable
private fun RowScope.ValueCell(value: String?) {
    Text(
        text = value ?: stringResource(R.string.value_not_set),
        modifier = Modifier.weight(VALUE_WEIGHT),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** "5 mo", short enough to stay on one line in a table column. */
@Composable
private fun Interval.shortLabel(): String = stringResource(
    when (unit) {
        IntervalUnit.DAYS -> R.string.interval_short_days
        IntervalUnit.WEEKS -> R.string.interval_short_weeks
        IntervalUnit.MONTHS -> R.string.interval_short_months
        IntervalUnit.YEARS -> R.string.interval_short_years
    },
    value,
)

private val HORIZONTAL_PADDING = 16.dp
private val COLUMN_SPACING = 8.dp
private const val SERVICE_WEIGHT = 4f
private const val VALUE_WEIGHT = 2f

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
            onLogService = {},
            onDeleteItem = {},
            onDeleteErrorShown = {},
            onRetry = {},
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
                    MaintenanceItem(
                        id = "m-3",
                        vehicleId = "v-1",
                        name = "Brake fluid flush",
                        recurrence = Interval(2, IntervalUnit.YEARS),
                        reminder = Interval(22, IntervalUnit.MONTHS),
                    ),
                ),
            ),
            onEditVehicle = {},
            onAddItem = {},
            onEditItem = {},
            onLogService = {},
            onDeleteItem = {},
            onDeleteErrorShown = {},
            onRetry = {},
            onBack = {},
        )
    }
}

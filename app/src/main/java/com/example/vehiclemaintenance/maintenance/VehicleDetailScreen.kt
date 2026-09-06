package com.example.vehiclemaintenance.maintenance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.servicelog.VehicleCostTotals
import com.example.vehiclemaintenance.servicelog.YearCost
import com.example.vehiclemaintenance.servicelog.formatCost
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.Vehicle
import java.time.LocalDate

@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    onEditVehicle: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (String) -> Unit,
    onLogService: (String) -> Unit,
    onLogRepair: () -> Unit,
    onViewHistory: () -> Unit,
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
        onLogRepair = onLogRepair,
        onViewHistory = onViewHistory,
        onDeleteItem = viewModel::deleteItem,
        onDeleteErrorShown = viewModel::dismissDeleteError,
        onNewlyOverdueShown = viewModel::dismissNewlyOverdue,
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
    onLogRepair: () -> Unit,
    onViewHistory: () -> Unit,
    onDeleteItem: (String) -> Unit,
    onDeleteErrorShown: () -> Unit,
    onNewlyOverdueShown: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vehicle = uiState.vehicle
    // The sheet and the dialog track an id, not the item, so a concurrent edit cannot show stale
    // text; the name is read back out of the current list each recomposition.
    var actionsForItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmingDeletionOfItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var showingCostTotals by rememberSaveable { mutableStateOf(false) }
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

            else -> Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                VehicleActionsRow(
                    onViewHistory = onViewHistory,
                    onLogRepair = onLogRepair,
                    onEditVehicle = onEditVehicle,
                )
                HorizontalDivider()
                CostTotalsRow(
                    totals = uiState.costTotals,
                    onClick = { showingCostTotals = true },
                )
                HorizontalDivider()
                if (uiState.rows.isEmpty()) {
                    CenteredColumn {
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
                } else {
                    MaintenanceItemTable(
                        rows = uiState.rows,
                        onOpenItemActions = { actionsForItemId = it },
                    )
                }
            }
        }
    }

    val actionsRow = uiState.rows.firstOrNull { it.item.id == actionsForItemId }
    if (actionsRow != null) {
        MaintenanceItemActionsSheet(
            itemName = actionsRow.item.name,
            status = actionsRow.status,
            onLogService = {
                actionsForItemId = null
                onLogService(actionsRow.item.id)
            },
            onEdit = {
                actionsForItemId = null
                onEditItem(actionsRow.item.id)
            },
            onDelete = {
                actionsForItemId = null
                confirmingDeletionOfItemId = actionsRow.item.id
            },
            onDismiss = { actionsForItemId = null },
        )
    }

    val deletingRow = uiState.rows.firstOrNull { it.item.id == confirmingDeletionOfItemId }
    if (deletingRow != null) {
        DeleteItemDialog(
            itemName = deletingRow.item.name,
            onConfirm = {
                confirmingDeletionOfItemId = null
                onDeleteItem(deletingRow.item.id)
            },
            onDismiss = { confirmingDeletionOfItemId = null },
        )
    }

    // Guarded on the current totals so the breakdown closes if the last costed entry goes away.
    if (showingCostTotals && uiState.costTotals.byYear.isNotEmpty()) {
        CostTotalsSheet(
            totals = uiState.costTotals,
            onDismiss = { showingCostTotals = false },
        )
    }

    if (uiState.newlyOverdueByMileage.isNotEmpty()) {
        NewlyOverdueDialog(
            itemNames = uiState.newlyOverdueByMileage,
            onDismiss = onNewlyOverdueShown,
        )
    }
}

/** The app cannot read an odometer, so a new reading is the only moment mileage can go overdue. */
@Composable
private fun NewlyOverdueDialog(
    itemNames: List<String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = {
            Text(
                pluralStringResource(
                    R.plurals.newly_overdue_title,
                    itemNames.size,
                    itemNames.size,
                ),
            )
        },
        text = { Text(itemNames.joinToString(separator = "\n")) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
    )
}

/** Scrolls sideways so a large font scale cannot clip an action off the screen. */
@Composable
private fun VehicleActionsRow(
    onViewHistory: () -> Unit,
    onLogRepair: () -> Unit,
    onEditVehicle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TextButton(onClick = onViewHistory) { Text(stringResource(R.string.service_history)) }
        TextButton(onClick = onLogRepair) { Text(stringResource(R.string.log_repair)) }
        TextButton(onClick = onEditVehicle) { Text(stringResource(R.string.edit_vehicle)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaintenanceItemActionsSheet(
    itemName: String,
    status: MaintenanceItemStatus,
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
            ItemStatusDetail(status)
            HorizontalDivider()
            SheetAction(stringResource(R.string.item_action_log_service), onLogService)
            SheetAction(stringResource(R.string.edit_maintenance_item), onEdit)
            SheetAction(stringResource(R.string.delete_maintenance_item), onDelete)
        }
    }
}

/**
 * Derived from the service log on every emission, so a newly logged cost lands here with no
 * refresh. A vehicle with nothing costed says so rather than claiming it has been free to run.
 */
@Composable
private fun CostTotalsRow(
    totals: VehicleCostTotals,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCosts = totals.byYear.isNotEmpty()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (hasCosts) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 48.dp)
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(COLUMN_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.cost_total_label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (hasCosts) {
                formatCost(totals.allTime)
            } else {
                stringResource(R.string.cost_totals_none)
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
        )
    }
}

/** Scrolls, because a long history can list more years than a sheet has room for. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CostTotalsSheet(
    totals: VehicleCostTotals,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.cost_totals_title),
                modifier = Modifier.padding(horizontal = HORIZONTAL_PADDING, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = HORIZONTAL_PADDING)
                    .padding(bottom = 12.dp),
            ) {
                totals.byYear.forEach { year ->
                    DetailLine(
                        label = stringResource(R.string.cost_year, year.year),
                        value = formatCost(year.total),
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                DetailLine(
                    label = stringResource(R.string.cost_total_label),
                    value = formatCost(totals.allTime),
                )
            }
        }
    }
}

/** The full derived picture for one item, which the table row has no room to spell out. */
@Composable
private fun ItemStatusDetail(status: MaintenanceItemStatus, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING)
            .padding(bottom = 12.dp),
    ) {
        DetailLine(stringResource(R.string.detail_status), statusLabel(status.status))
        DetailLine(
            label = stringResource(R.string.detail_next_reminder),
            value = status.nextReminderDate?.let { formatMediumDate(it) },
        )
        DetailLine(
            label = stringResource(R.string.detail_due_date),
            value = status.dueDate?.let { formatMediumDate(it) },
        )
        DetailLine(
            label = stringResource(R.string.detail_mileage_due),
            value = status.mileageDue?.let { milesLabel(it) },
        )
        DetailLine(
            label = stringResource(R.string.detail_miles_left),
            value = status.milesLeft?.let { milesLabel(it) },
        )
    }
}

@Composable
private fun DetailLine(label: String, value: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: stringResource(R.string.value_not_set),
            style = MaterialTheme.typography.bodyMedium,
        )
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
    rows: List<MaintenanceItemRow>,
    onOpenItemActions: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        MaintenanceItemHeader()
        HorizontalDivider()
        LazyColumn {
            items(rows, key = { it.item.id }) { row ->
                MaintenanceTableRow(row = row, onClick = { onOpenItemActions(row.item.id) })
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
        HeaderCell(stringResource(R.string.column_next_due), VALUE_WEIGHT, TextAlign.End)
        HeaderCell(stringResource(R.string.column_miles_left), VALUE_WEIGHT, TextAlign.End)
        HeaderCell(stringResource(R.string.column_status), STATUS_WEIGHT, TextAlign.End)
    }
}

/** Two lines, so a two word heading wraps instead of ellipsizing in a narrow column. */
@Composable
private fun RowScope.HeaderCell(text: String, weight: Float, textAlign: TextAlign) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MaintenanceTableRow(
    row: MaintenanceItemRow,
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
        Column(modifier = Modifier.weight(SERVICE_WEIGHT)) {
            Text(
                text = row.item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val intervals = row.item.intervalSummary()
            if (intervals != null) {
                Text(
                    text = intervals,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ValueCell(row.status.nextDueDate?.let { formatShortDate(it) })
        ValueCell(row.status.milesLeft?.let { formatMileage(it) })
        StatusCell(row.status.status)
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

/** The label always states the status, so color is never the only thing carrying it. */
@Composable
private fun RowScope.StatusCell(status: MaintenanceStatus) {
    Text(
        text = statusLabel(status),
        modifier = Modifier.weight(STATUS_WEIGHT),
        style = MaterialTheme.typography.bodyMedium,
        color = when (status) {
            MaintenanceStatus.OVERDUE -> MaterialTheme.colorScheme.error
            MaintenanceStatus.DUE -> MaterialTheme.colorScheme.tertiary
            MaintenanceStatus.OK, MaintenanceStatus.NONE ->
                MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun statusLabel(status: MaintenanceStatus): String = stringResource(
    when (status) {
        MaintenanceStatus.OK -> R.string.status_ok
        MaintenanceStatus.DUE -> R.string.status_due
        MaintenanceStatus.OVERDUE -> R.string.status_overdue
        MaintenanceStatus.NONE -> R.string.value_not_set
    },
)

/** "5,000 mi - 6 mo": the settings behind the row, kept under the name so the columns fit. */
@Composable
private fun MaintenanceItem.intervalSummary(): String? {
    val miles = mileageInterval?.let { milesLabel(it) }
    val time = recurrence?.shortLabel()
    return when {
        miles != null && time != null ->
            stringResource(R.string.interval_summary_both, miles, time)

        else -> miles ?: time
    }
}

@Composable
private fun milesLabel(miles: Int): String =
    stringResource(R.string.interval_short_miles, formatMileage(miles))

/** "5 mo", short enough to stay on one line under the service name. */
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
private const val SERVICE_WEIGHT = 3.5f
private const val VALUE_WEIGHT = 2f
private const val STATUS_WEIGHT = 2.5f

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

private val previewToday = LocalDate.of(2026, 9, 6)

private fun previewRow(
    id: String,
    name: String,
    mileageInterval: Int? = null,
    recurrence: Interval? = null,
    reminder: Interval = Interval(5, IntervalUnit.MONTHS),
    lastDoneDate: LocalDate? = LocalDate.of(2026, 3, 1),
    lastDoneMileage: Int? = null,
    odometer: Int? = null,
): MaintenanceItemRow {
    val item = MaintenanceItem(
        id = id,
        vehicleId = "v-1",
        name = name,
        mileageInterval = mileageInterval,
        recurrence = recurrence,
        reminder = reminder,
        lastDoneDate = lastDoneDate,
        lastDoneMileage = lastDoneMileage,
    )
    return MaintenanceItemRow(item, statusOf(item, odometer, previewToday))
}

private val previewRows = listOf(
    // Overdue by mileage, with both intervals under the name.
    previewRow(
        id = "m-1",
        name = "Oil change",
        mileageInterval = 5_000,
        recurrence = Interval(6, IntervalUnit.MONTHS),
        lastDoneMileage = 42_000,
        odometer = 48_000,
    ),
    // Mileage only, so the reminder is what turns it Due.
    previewRow(
        id = "m-2",
        name = "Tire rotation",
        mileageInterval = 7_500,
        reminder = Interval(90, IntervalUnit.DAYS),
    ),
    // Still inside every interval.
    previewRow(
        id = "m-3",
        name = "Brake fluid flush",
        recurrence = Interval(2, IntervalUnit.YEARS),
        reminder = Interval(22, IntervalUnit.MONTHS),
    ),
    // Last done date cleared and no mileage baseline, so nothing is computable.
    previewRow(id = "m-4", name = "Cabin air filter", lastDoneDate = null),
)

private val previewCostTotals = VehicleCostTotals(
    allTime = 184_297L,
    byYear = listOf(YearCost(2026, 64_99L), YearCost(2025, 98_50L), YearCost(2024, 20_848L)),
)

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
            onLogRepair = {},
            onViewHistory = {},
            onDeleteItem = {},
            onDeleteErrorShown = {},
            onNewlyOverdueShown = {},
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
                rows = previewRows,
                costTotals = previewCostTotals,
            ),
            onEditVehicle = {},
            onAddItem = {},
            onEditItem = {},
            onLogService = {},
            onLogRepair = {},
            onViewHistory = {},
            onDeleteItem = {},
            onDeleteErrorShown = {},
            onNewlyOverdueShown = {},
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleDetailNoCostsPreview() {
    VehicleMaintenanceTheme {
        VehicleDetailContent(
            uiState = VehicleDetailUiState(
                isLoading = false,
                vehicle = previewVehicle,
                rows = previewRows,
            ),
            onEditVehicle = {},
            onAddItem = {},
            onEditItem = {},
            onLogService = {},
            onLogRepair = {},
            onViewHistory = {},
            onDeleteItem = {},
            onDeleteErrorShown = {},
            onNewlyOverdueShown = {},
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleDetailNewlyOverduePreview() {
    VehicleMaintenanceTheme {
        VehicleDetailContent(
            uiState = VehicleDetailUiState(
                isLoading = false,
                vehicle = previewVehicle,
                rows = previewRows,
                newlyOverdueByMileage = listOf("Oil change", "Air filter"),
            ),
            onEditVehicle = {},
            onAddItem = {},
            onEditItem = {},
            onLogService = {},
            onLogRepair = {},
            onViewHistory = {},
            onDeleteItem = {},
            onDeleteErrorShown = {},
            onNewlyOverdueShown = {},
            onRetry = {},
            onBack = {},
        )
    }
}

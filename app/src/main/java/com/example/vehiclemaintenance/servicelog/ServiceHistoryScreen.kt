package com.example.vehiclemaintenance.servicelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.maintenance.formatMediumDate
import com.example.vehiclemaintenance.maintenance.formatMileage
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.Vehicle
import java.time.LocalDate

@Composable
fun ServiceHistoryScreen(
    vehicleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ServiceHistoryViewModel = viewModel(
        key = vehicleId,
        factory = ServiceHistoryViewModel.factory(vehicleId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    ServiceHistoryContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryContent(
    uiState: ServiceHistoryUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.service_history),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
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

            uiState.vehicleNotFound || uiState.vehicle == null -> CenteredColumn(
                Modifier.padding(innerPadding),
            ) {
                Text(stringResource(R.string.vehicle_not_found))
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            }

            uiState.history.years.isEmpty() -> CenteredColumn(Modifier.padding(innerPadding)) {
                Text(
                    text = stringResource(R.string.history_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.history_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(modifier = Modifier.padding(innerPadding)) {
                item(key = "summary") {
                    CostSummary(uiState.history)
                    HorizontalDivider()
                }
                uiState.history.years.forEach { yearSection(it) }
            }
        }
    }
}

/** A vehicle with nothing costed says so rather than claiming it has been free to run. */
@Composable
private fun CostSummary(history: ServiceHistory, modifier: Modifier = Modifier) {
    var showingAverageInfo by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        val allTime = history.allTime
        if (allTime == null) {
            AmountRow(
                label = stringResource(R.string.cost_all_time),
                value = stringResource(R.string.cost_totals_none),
                style = MaterialTheme.typography.titleLarge,
                valueStyle = MaterialTheme.typography.bodyLarge,
                valueColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            AmountRow(
                label = stringResource(R.string.cost_all_time),
                value = formatCost(allTime),
                style = MaterialTheme.typography.titleLarge,
            )
            AmountRow(
                label = stringResource(R.string.cost_average_per_year),
                value = history.averagePerYear?.let { formatCost(it) }
                    ?: stringResource(R.string.value_not_set),
                style = MaterialTheme.typography.bodyLarge,
                labelAction = {
                    IconButton(onClick = { showingAverageInfo = true }) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.cost_average_info),
                        )
                    }
                },
            )
        }
    }
    if (showingAverageInfo) {
        AverageInfoDialog(onDismiss = { showingAverageInfo = false })
    }
}

@Composable
private fun AverageInfoDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.cost_average_per_year)) },
        text = { Text(stringResource(R.string.cost_average_info_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        },
    )
}

private fun LazyListScope.yearSection(year: HistoryYear) {
    item(key = "year-${year.year}") {
        AmountRow(
            label = stringResource(R.string.cost_year, year.year),
            value = year.total?.let { formatCost(it) } ?: stringResource(R.string.value_not_set),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    items(year.entries, key = { it.id }) { entry ->
        HistoryEntryRow(entry)
    }
    item(key = "year-${year.year}-end") { HorizontalDivider() }
}

@Composable
private fun AmountRow(
    label: String,
    value: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = style,
    valueColor: Color = Color.Unspecified,
    labelAction: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, style = style)
            labelAction?.invoke()
        }
        Text(text = value, style = valueStyle, color = valueColor, textAlign = TextAlign.End)
    }
}

@Composable
private fun HistoryEntryRow(entry: ServiceLogEntry, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            SecondaryText(
                stringResource(
                    R.string.history_date_odometer,
                    formatMediumDate(entry.date),
                    formatMileage(entry.odometer),
                ),
            )
            entry.notes?.takeIf { it.isNotBlank() }?.let { SecondaryText(it) }
        }
        entry.cost?.let {
            Text(
                text = formatCost(it),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun SecondaryText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private val HORIZONTAL_PADDING = 16.dp

private val previewVehicle = Vehicle("v-1", 2014, "Toyota", "Tacoma", "4.0L V6")

private val previewEntries = listOf(
    ServiceLogEntry(
        id = "s-4",
        vehicleId = "v-1",
        maintenanceItemId = "m-1",
        description = "Oil change",
        date = LocalDate.of(2026, 9, 5),
        odometer = 48000,
        cost = 6499,
        notes = "Shop said the belts look fine",
    ),
    ServiceLogEntry(
        id = "s-3",
        vehicleId = "v-1",
        description = "Replaced the alternator",
        date = LocalDate.of(2026, 6, 12),
        odometer = 45120,
        cost = 78250,
    ),
    ServiceLogEntry(
        id = "s-2",
        vehicleId = "v-1",
        maintenanceItemId = "m-2",
        description = "Tire rotation",
        date = LocalDate.of(2026, 3, 1),
        odometer = 42000,
    ),
    ServiceLogEntry(
        id = "s-1",
        vehicleId = "v-1",
        maintenanceItemId = "m-3",
        description = "Brake pads",
        date = LocalDate.of(2025, 3, 1),
        odometer = 38000,
        cost = 21000,
    ),
)

@Preview(showBackground = true)
@Composable
private fun ServiceHistoryPreview() {
    VehicleMaintenanceTheme {
        ServiceHistoryContent(
            uiState = ServiceHistoryUiState(
                isLoading = false,
                vehicle = previewVehicle,
                history = serviceHistoryOf(previewEntries),
            ),
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceHistoryUncostedPreview() {
    VehicleMaintenanceTheme {
        ServiceHistoryContent(
            uiState = ServiceHistoryUiState(
                isLoading = false,
                vehicle = previewVehicle,
                history = serviceHistoryOf(previewEntries.filter { it.cost == null }),
            ),
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ServiceHistoryEmptyPreview() {
    VehicleMaintenanceTheme {
        ServiceHistoryContent(
            uiState = ServiceHistoryUiState(isLoading = false, vehicle = previewVehicle),
            onRetry = {},
            onBack = {},
        )
    }
}

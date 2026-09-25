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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.maintenance.formatMediumDate
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.Vehicle
import java.time.LocalDate

@Composable
fun CostBreakdownScreen(
    vehicleId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CostBreakdownViewModel = viewModel(
        key = vehicleId,
        factory = CostBreakdownViewModel.factory(vehicleId),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()

    CostBreakdownContent(
        uiState = uiState,
        onRetry = viewModel::refresh,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostBreakdownContent(
    uiState: CostBreakdownUiState,
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
                        text = stringResource(R.string.cost_total_label),
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

            // A vehicle with nothing costed says so rather than claiming it has been free to run.
            uiState.totals.byYear.isEmpty() -> CenteredColumn(Modifier.padding(innerPadding)) {
                Text(
                    text = stringResource(R.string.cost_totals_none),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(R.string.cost_breakdown_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(modifier = Modifier.padding(innerPadding)) {
                item(key = "all-time") {
                    AmountRow(
                        label = stringResource(R.string.cost_all_time),
                        amount = uiState.totals.allTime,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    HorizontalDivider()
                }
                uiState.totals.byYear.forEach { yearSection(it) }
            }
        }
    }
}

private fun LazyListScope.yearSection(year: YearCost) {
    item(key = "year-${year.year}") {
        AmountRow(
            label = stringResource(R.string.cost_year, year.year),
            amount = year.total,
            style = MaterialTheme.typography.titleMedium,
        )
    }
    items(year.entries, key = { it.id }) { entry ->
        CostEntryRow(entry)
    }
    item(key = "year-${year.year}-end") { HorizontalDivider() }
}

@Composable
private fun AmountRow(
    label: String,
    amount: Long,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f), style = style)
        Text(text = formatCost(amount), style = style, textAlign = TextAlign.End)
    }
}

@Composable
private fun CostEntryRow(entry: ServiceLogEntry, modifier: Modifier = Modifier) {
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = formatMediumDate(entry.date),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

@Preview(showBackground = true)
@Composable
private fun CostBreakdownPreview() {
    VehicleMaintenanceTheme {
        CostBreakdownContent(
            uiState = CostBreakdownUiState(
                isLoading = false,
                vehicle = previewVehicle,
                totals = costTotalsOf(
                    listOf(
                        ServiceLogEntry(
                            id = "s-3",
                            vehicleId = "v-1",
                            maintenanceItemId = "m-1",
                            description = "Oil change",
                            date = LocalDate.of(2026, 9, 5),
                            odometer = 48000,
                            cost = 6499,
                        ),
                        ServiceLogEntry(
                            id = "s-2",
                            vehicleId = "v-1",
                            description = "Replaced the alternator",
                            date = LocalDate.of(2026, 6, 12),
                            odometer = 45120,
                            cost = 78250,
                        ),
                        ServiceLogEntry(
                            id = "s-1",
                            vehicleId = "v-1",
                            maintenanceItemId = "m-2",
                            description = "Brake pads",
                            date = LocalDate.of(2025, 3, 1),
                            odometer = 42000,
                            cost = 21000,
                        ),
                    ),
                ),
            ),
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CostBreakdownEmptyPreview() {
    VehicleMaintenanceTheme {
        CostBreakdownContent(
            uiState = CostBreakdownUiState(isLoading = false, vehicle = previewVehicle),
            onRetry = {},
            onBack = {},
        )
    }
}

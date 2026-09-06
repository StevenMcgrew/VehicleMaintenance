package com.example.vehiclemaintenance.servicelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
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
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
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

            uiState.entries.isEmpty() -> CenteredColumn(Modifier.padding(innerPadding)) {
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
                items(uiState.entries, key = { it.id }) { entry ->
                    ServiceHistoryRow(entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ServiceHistoryRow(entry: ServiceLogEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HORIZONTAL_PADDING, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = entry.description,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailText(entry.date.toString())
            DetailText(stringResource(R.string.history_odometer, formatMileage(entry.odometer)))
            entry.cost?.let { DetailText(formatCost(it)) }
        }
        entry.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
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

@Preview(showBackground = true)
@Composable
private fun ServiceHistoryPreview() {
    VehicleMaintenanceTheme {
        ServiceHistoryContent(
            uiState = ServiceHistoryUiState(
                isLoading = false,
                vehicle = previewVehicle,
                entries = listOf(
                    ServiceLogEntry(
                        id = "s-3",
                        vehicleId = "v-1",
                        maintenanceItemId = "m-1",
                        description = "Oil change",
                        date = LocalDate.of(2026, 9, 5),
                        odometer = 48000,
                        cost = 6499,
                        notes = "Shop said the belts look fine",
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
                        description = "Tire rotation",
                        date = LocalDate.of(2026, 3, 1),
                        odometer = 42000,
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
private fun ServiceHistoryEmptyPreview() {
    VehicleMaintenanceTheme {
        ServiceHistoryContent(
            uiState = ServiceHistoryUiState(isLoading = false, vehicle = previewVehicle),
            onRetry = {},
            onBack = {},
        )
    }
}

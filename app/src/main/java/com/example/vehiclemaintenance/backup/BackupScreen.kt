package com.example.vehiclemaintenance.backup

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.data.MaintenanceStore
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE),
    ) { target -> if (target != null) viewModel.export(target) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { source -> if (source != null) viewModel.prepareImport(source) }

    BackupContent(
        uiState = uiState,
        onExport = {
            try {
                exportLauncher.launch(viewModel.suggestedFileName())
            } catch (e: ActivityNotFoundException) {
                viewModel.reportPickerUnavailable()
            }
        },
        onImport = {
            try {
                importLauncher.launch(IMPORT_MIME_TYPES)
            } catch (e: ActivityNotFoundException) {
                viewModel.reportPickerUnavailable()
            }
        },
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onMessageShown = viewModel::messageShown,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupContent(
    uiState: BackupUiState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onMessageShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = uiState.message?.let { messageText(it) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_export_explanation),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onExport,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_export))
            }
            Text(
                text = stringResource(R.string.backup_import_explanation),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = onImport,
                enabled = !uiState.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_import))
            }
            if (uiState.isBusy) {
                CircularProgressIndicator()
            }
        }
    }

    val pending = uiState.pendingImport
    if (pending != null) {
        ConfirmImportDialog(
            store = pending,
            onConfirm = onConfirmImport,
            onDismiss = onCancelImport,
        )
    }
}

@Composable
private fun ConfirmImportDialog(
    store: MaintenanceStore,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_confirm_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(countsSummary(store))
                Text(stringResource(R.string.import_confirm_warning))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.import_confirm_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun countsSummary(store: MaintenanceStore): String {
    val vehicles = store.vehicles.size
    val items = store.maintenanceItems.size
    val records = store.serviceLogEntries.size
    return stringResource(
        R.string.import_confirm_counts,
        pluralStringResource(R.plurals.import_vehicle_count, vehicles, vehicles),
        pluralStringResource(R.plurals.import_item_count, items, items),
        pluralStringResource(R.plurals.import_record_count, records, records),
    )
}

@Composable
private fun messageText(message: BackupMessage): String = when (message) {
    BackupMessage.ExportSucceeded -> stringResource(R.string.backup_export_succeeded)
    BackupMessage.ExportFailed -> stringResource(R.string.backup_export_failed)
    BackupMessage.StoreUnavailable -> stringResource(R.string.backup_store_unavailable)
    BackupMessage.PickerUnavailable -> stringResource(R.string.backup_picker_unavailable)
    BackupMessage.ImportSucceeded -> stringResource(R.string.backup_import_succeeded)
    BackupMessage.ImportFailed -> stringResource(R.string.backup_import_failed)
    BackupMessage.FileUnreadable -> stringResource(R.string.backup_file_unreadable)
    BackupMessage.FileTooLarge -> stringResource(R.string.backup_file_too_large)
    BackupMessage.FileInvalid -> stringResource(R.string.backup_file_invalid)
    is BackupMessage.FileFromAnotherVersion ->
        stringResource(R.string.backup_file_other_version, message.version)
}

private const val EXPORT_MIME_TYPE = "application/json"

/**
 * Providers label an exported .json inconsistently (application/json, octet-stream, text/plain), so
 * a narrow filter would grey out the user's own backup. The parser is the real gate.
 */
private val IMPORT_MIME_TYPES = arrayOf("*/*")

@Preview(showBackground = true)
@Composable
private fun BackupPreview() {
    VehicleMaintenanceTheme {
        BackupContent(
            uiState = BackupUiState(),
            onExport = {},
            onImport = {},
            onConfirmImport = {},
            onCancelImport = {},
            onMessageShown = {},
            onBack = {},
        )
    }
}

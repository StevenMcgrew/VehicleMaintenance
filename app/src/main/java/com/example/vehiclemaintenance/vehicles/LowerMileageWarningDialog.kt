package com.example.vehiclemaintenance.vehicles

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.maintenance.formatMileage
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

/** A reading waiting on the owner's OK because it is below one recorded before. */
data class LowerMileageWarning(val entered: Int, val highestKnown: Int)

@Composable
fun LowerMileageWarningDialog(
    warning: LowerMileageWarning,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.lower_mileage_title)) },
        text = {
            Text(
                stringResource(
                    R.string.lower_mileage_message,
                    formatMileage(warning.entered),
                    formatMileage(warning.highestKnown),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Preview
@Composable
private fun LowerMileageWarningDialogPreview() {
    VehicleMaintenanceTheme {
        LowerMileageWarningDialog(
            warning = LowerMileageWarning(entered = 44_000, highestKnown = 45_000),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

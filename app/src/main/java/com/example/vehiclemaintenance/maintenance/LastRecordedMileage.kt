package com.example.vehiclemaintenance.maintenance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme
import com.example.vehiclemaintenance.vehicles.LowerMileageWarningDialog
import com.example.vehiclemaintenance.vehicles.VehicleFieldError

/** Everything the Update mileage flow reports back to its view model. */
data class MileageActions(
    val onStart: () -> Unit = {},
    val onTextChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onConfirmLower: () -> Unit = {},
    val onDismissLower: () -> Unit = {},
    val onCancel: () -> Unit = {},
    val onSaveErrorShown: () -> Unit = {},
)

/** Wraps the button under the text so a large font scale cannot clip it off the screen. */
@Composable
fun LastRecordedMileageRow(
    mileage: Int?,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = lastRecordedMileageText(mileage),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onUpdate) { Text(stringResource(R.string.update_mileage)) }
    }
}

@Composable
fun lastRecordedMileageText(mileage: Int?): String = if (mileage == null) {
    stringResource(R.string.last_recorded_mileage_none)
} else {
    stringResource(R.string.last_recorded_mileage, formatMileage(mileage))
}

@Composable
fun UpdateMileageDialog(
    editor: MileageEditorState,
    actions: MileageActions,
    modifier: Modifier = Modifier,
) {
    val warning = editor.lowerMileageWarning
    if (warning != null) {
        LowerMileageWarningDialog(
            warning = warning,
            onConfirm = actions.onConfirmLower,
            onDismiss = actions.onDismissLower,
            modifier = modifier,
        )
        return
    }
    val error = editor.error?.let { mileageErrorText(it) }
    AlertDialog(
        onDismissRequest = actions.onCancel,
        modifier = modifier,
        title = { Text(stringResource(R.string.update_mileage_title)) },
        text = {
            OutlinedTextField(
                value = editor.text,
                onValueChange = actions.onTextChange,
                label = { Text(stringResource(R.string.update_mileage_label)) },
                isError = error != null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { actions.onSubmit() }),
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (error != null) Modifier.semantics { error(error) } else Modifier),
            )
        },
        confirmButton = {
            TextButton(onClick = actions.onSubmit, enabled = !editor.isSaving) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = actions.onCancel) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun mileageErrorText(error: VehicleFieldError): String = when (error) {
    VehicleFieldError.REQUIRED -> stringResource(R.string.error_required)
    else -> stringResource(R.string.error_non_negative_number)
}

@Preview(showBackground = true)
@Composable
private fun LastRecordedMileageRowPreview() {
    VehicleMaintenanceTheme {
        LastRecordedMileageRow(mileage = 45_000, onUpdate = {})
    }
}

@Preview
@Composable
private fun UpdateMileageDialogPreview() {
    VehicleMaintenanceTheme {
        UpdateMileageDialog(
            editor = MileageEditorState(
                text = "45.5",
                error = VehicleFieldError.MILEAGE_NOT_A_NUMBER,
            ),
            actions = MileageActions(),
        )
    }
}

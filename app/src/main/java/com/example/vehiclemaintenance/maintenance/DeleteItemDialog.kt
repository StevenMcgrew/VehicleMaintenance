package com.example.vehiclemaintenance.maintenance

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.brandTextButtonColors
import com.example.vehiclemaintenance.ui.theme.LocalBrandColors

@Composable
fun DeleteItemDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalBrandColors.current.popupContainer,
        title = { Text(stringResource(R.string.delete_item_title, itemName)) },
        text = { Text(stringResource(R.string.delete_item_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = brandTextButtonColors()) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = brandTextButtonColors()) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

package com.example.vehiclemaintenance.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

/**
 * The Material 3 Expressive extra-small button (32dp, fully round), built by hand because the
 * material3 version on the Compose BOM does not ship the Expressive size tokens yet. The button
 * still pads its touch target out to the 48dp minimum. The icon is decorative because the label
 * already names the action.
 */
@Composable
fun ExtraSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Preview(showBackground = true)
@Composable
private fun ExtraSmallButtonPreview() {
    VehicleMaintenanceTheme {
        Row(Modifier.padding(16.dp)) {
            ExtraSmallButton(text = "Add item", onClick = {}, icon = Icons.Filled.Add)
            Spacer(Modifier.width(8.dp))
            ExtraSmallButton(text = "Service history", onClick = {})
        }
    }
}

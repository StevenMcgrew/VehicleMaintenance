package com.example.vehiclemaintenance.vehicles

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.playstore.shareApp
import com.example.vehiclemaintenance.settings.ThemeMode
import com.example.vehiclemaintenance.ui.brandIconButtonColors
import com.example.vehiclemaintenance.ui.brandMenuItemColors
import com.example.vehiclemaintenance.ui.brandSegmentedButtonColors
import com.example.vehiclemaintenance.ui.theme.LocalBrandColors
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

/** The three-dot menu at the top right of the vehicle list. */
@Composable
fun VehicleListMenu(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }, colors = brandIconButtonColors()) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = LocalBrandColors.current.popupContainer,
        ) {
            VehicleListMenuItems(
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onOpenBackup = {
                    expanded = false
                    onOpenBackup()
                },
                onOpenPrivacy = {
                    expanded = false
                    onOpenPrivacy()
                },
                onShareApp = {
                    expanded = false
                    shareApp(context)
                },
            )
        }
    }
}

@Composable
private fun VehicleListMenuItems(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onShareApp: () -> Unit,
) {
    ThemeChoice(
        themeMode = themeMode,
        onThemeModeChange = onThemeModeChange,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.backup_action)) },
        onClick = onOpenBackup,
        colors = brandMenuItemColors(),
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.privacy_action)) },
        onClick = onOpenPrivacy,
        colors = brandMenuItemColors(),
    )
    DropdownMenuItem(
        text = { Text(stringResource(R.string.share_app_action)) },
        onClick = onShareApp,
        colors = brandMenuItemColors(),
    )
}

/**
 * Picking a mode applies it at once and leaves the menu open, so the change is visible before the
 * menu is dismissed. The checkmark icon is dropped so three labels fit the menu's width.
 */
@Composable
private fun ThemeChoice(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.theme_label),
            style = MaterialTheme.typography.labelLarge,
        )
        SingleChoiceSegmentedButtonRow {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = mode == themeMode,
                    onClick = { onThemeModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    colors = brandSegmentedButtonColors(),
                    icon = {},
                    label = { Text(stringResource(mode.label)) },
                )
            }
        }
    }
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VehicleListMenuItemsPreview() {
    VehicleMaintenanceTheme {
        Surface(color = LocalBrandColors.current.popupContainer) {
            Column {
                VehicleListMenuItems(
                    themeMode = ThemeMode.SYSTEM,
                    onThemeModeChange = {},
                    onOpenBackup = {},
                    onOpenPrivacy = {},
                    onShareApp = {},
                )
            }
        }
    }
}

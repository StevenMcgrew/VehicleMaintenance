package com.example.vehiclemaintenance.help

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vehiclemaintenance.R
import com.example.vehiclemaintenance.ui.brandIconButtonColors
import com.example.vehiclemaintenance.ui.theme.VehicleMaintenanceTheme

private data class HelpSection(@StringRes val heading: Int, @StringRes val body: Int)

private val helpSections = listOf(
    HelpSection(R.string.help_getting_started_heading, R.string.help_getting_started_body),
    HelpSection(R.string.help_intervals_heading, R.string.help_intervals_body),
    HelpSection(R.string.help_last_done_heading, R.string.help_last_done_body),
    HelpSection(R.string.help_status_heading, R.string.help_status_body),
    HelpSection(R.string.help_mileage_heading, R.string.help_mileage_body),
    HelpSection(R.string.help_logging_heading, R.string.help_logging_body),
    HelpSection(R.string.help_reminders_heading, R.string.help_reminders_body),
    HelpSection(R.string.help_history_heading, R.string.help_history_body),
    HelpSection(R.string.help_backup_heading, R.string.help_backup_body),
)

/** Answers to common questions, as static text from resources so it works offline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, colors = brandIconButtonColors()) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            helpSections.forEach { section ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(section.heading),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(section.body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HelpScreenPreview() {
    VehicleMaintenanceTheme {
        HelpScreen(onBack = {})
    }
}

package com.example.vehiclemaintenance.privacy

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

private data class PolicySection(@StringRes val heading: Int, @StringRes val body: Int)

/** In the same order as play-store/privacy-policy.html. */
private val policySections = listOf(
    PolicySection(R.string.privacy_stored_heading, R.string.privacy_stored_body),
    PolicySection(R.string.privacy_collected_heading, R.string.privacy_collected_body),
    PolicySection(R.string.privacy_backup_heading, R.string.privacy_backup_body),
    PolicySection(R.string.privacy_files_heading, R.string.privacy_files_body),
    PolicySection(R.string.privacy_reminders_heading, R.string.privacy_reminders_body),
    PolicySection(R.string.privacy_deleting_heading, R.string.privacy_deleting_body),
    PolicySection(R.string.privacy_children_heading, R.string.privacy_children_body),
    PolicySection(R.string.privacy_changes_heading, R.string.privacy_changes_body),
    PolicySection(R.string.privacy_contact_heading, R.string.privacy_contact_body),
)

/**
 * The privacy policy Play requires inside the app. It is static text from resources, so the screen
 * works offline and has nothing to load or fail.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
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
            Text(
                text = stringResource(R.string.privacy_effective),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.privacy_intro),
                style = MaterialTheme.typography.bodyMedium,
            )
            policySections.forEach { section ->
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

@Preview(showBackground = true)
@Composable
private fun PrivacyScreenPreview() {
    VehicleMaintenanceTheme {
        PrivacyScreen(onBack = {})
    }
}

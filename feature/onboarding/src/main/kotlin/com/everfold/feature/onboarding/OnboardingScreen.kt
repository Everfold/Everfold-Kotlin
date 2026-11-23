package com.everfold.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.model.ConnectionMode
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Everfold",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OnboardingModeOptions(
                selectedMode = state.selectedMode,
                onSelectMode = viewModel::selectMode,
            )
            if (state.selectedMode != ConnectionMode.LOCAL_ONLY) {
                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::updateAddress,
                    label = { Text(stringResource(R.string.onboarding_address_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OnboardingConnectButton(
                mode = state.selectedMode,
                isConnecting = state.isConnecting,
                onClick = viewModel::connect,
            )
        }
    }
}

@Composable
private fun OnboardingModeOptions(
    selectedMode: ConnectionMode,
    onSelectMode: (ConnectionMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ConnectionModeOption(
            title = stringResource(R.string.onboarding_mode_remote_title),
            subtitle = stringResource(R.string.onboarding_mode_remote_subtitle),
            icon = Icons.Rounded.Cloud,
            selected = selectedMode == ConnectionMode.REMOTE_AUTHORITY,
            onClick = { onSelectMode(ConnectionMode.REMOTE_AUTHORITY) },
        )
        ConnectionModeOption(
            title = stringResource(R.string.onboarding_mode_relay_title),
            subtitle = stringResource(R.string.onboarding_mode_relay_subtitle),
            icon = Icons.Rounded.Hub,
            selected = selectedMode == ConnectionMode.RELAY,
            onClick = { onSelectMode(ConnectionMode.RELAY) },
        )
        ConnectionModeOption(
            title = stringResource(R.string.onboarding_mode_local_title),
            subtitle = stringResource(R.string.onboarding_mode_local_subtitle),
            icon = Icons.Rounded.Smartphone,
            selected = selectedMode == ConnectionMode.LOCAL_ONLY,
            onClick = { onSelectMode(ConnectionMode.LOCAL_ONLY) },
        )
    }
}

@Composable
private fun OnboardingConnectButton(
    mode: ConnectionMode,
    isConnecting: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isConnecting,
        modifier = Modifier.fillMaxWidth().height(52.dp),
    ) {
        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(
                text =
                    stringResource(
                        if (mode == ConnectionMode.LOCAL_ONLY) {
                            R.string.onboarding_cta_local
                        } else {
                            R.string.onboarding_cta_connect
                        },
                    ),
            )
        }
    }
}

@Composable
private fun ConnectionModeOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border =
        if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        }
    val containerColor =
        if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        border = border,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

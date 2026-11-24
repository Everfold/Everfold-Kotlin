package com.everfold.feature.sessions.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.feature.sessions.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionContextDetailSheet(
    context: SessionRuntimeContext,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.session_context_inspector_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Text(
                text = stringResource(R.string.session_context_locked_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                ContextDetailFieldsCard(context = context)
            }
        }
    }
}

@Composable
private fun ContextDetailFieldsCard(context: SessionRuntimeContext) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (context.relayName.isNotBlank()) {
            ContextInspectorRow(label = stringResource(R.string.session_context_field_relay), value = context.relayName)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        ContextInspectorRow(
            label = stringResource(R.string.session_context_field_node),
            value =
                if (context.nodePlatform.isNotBlank()) {
                    "${context.nodeName} · ${context.nodePlatform}"
                } else {
                    context.nodeName
                },
        )
        if (context.workspaceName.isNotBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ContextInspectorRow(
                label = stringResource(R.string.session_context_field_workspace),
                value = context.workspaceName,
            )
        }
        if (context.workspaceFileScope.isNotBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ContextInspectorRow(
                label = stringResource(R.string.session_context_field_scope),
                value = context.workspaceFileScope,
                monospace = true,
            )
        }
        ContextBoundarySection(context = context)
    }
}

@Composable
private fun ColumnScope.ContextBoundarySection(context: SessionRuntimeContext) {
    context.workspacePermission?.let { permission ->
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.session_context_field_permission),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WorkspacePermissionBadge(permission = permission)
        }
    }
    if (context.workspaceCapabilities.isNotEmpty()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.session_context_field_capabilities),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WorkspaceCapabilityChips(capabilities = context.workspaceCapabilities)
        }
    }
}

@Composable
private fun ContextInspectorRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style =
                if (monospace) {
                    MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

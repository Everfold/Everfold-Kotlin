package com.everfold.feature.sessions.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.WorkspacePermissionLevel
import com.everfold.feature.sessions.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
            }
            Text(
                text = stringResource(R.string.session_context_locked_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ContextDetailFieldsCard(context = context)
        }
    }
}

@Composable
private fun ContextDetailFieldsCard(context: SessionRuntimeContext) {
    val entries = buildInspectorEntries(context)
    EverfoldGroupedList {
        entries.forEachIndexed { index, entry ->
            EverfoldGroupedItem(index = index, count = entries.size) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    entry()
                }
            }
        }
    }
}

private fun buildInspectorEntries(context: SessionRuntimeContext): List<@Composable () -> Unit> =
    buildList {
        if (context.relayName.isNotBlank()) {
            add {
                ContextInspectorRow(
                    label = stringResource(R.string.session_context_field_relay),
                    value = context.relayName,
                )
            }
        }
        val nodeValue =
            if (context.nodePlatform.isNotBlank()) {
                "${context.nodeName} · ${context.nodePlatform}"
            } else {
                context.nodeName
            }
        add { ContextInspectorRow(label = stringResource(R.string.session_context_field_node), value = nodeValue) }
        if (context.workspaceName.isNotBlank()) {
            add {
                ContextInspectorRow(
                    label = stringResource(R.string.session_context_field_workspace),
                    value = context.workspaceName,
                )
            }
        }
        if (context.workspaceFileScope.isNotBlank()) {
            add {
                ContextInspectorRow(
                    label = stringResource(R.string.session_context_field_scope),
                    value = context.workspaceFileScope,
                    monospace = true,
                )
            }
        }
        context.workspacePermission?.let { permission ->
            add { ContextPermissionRow(permission = permission) }
        }
        if (context.workspaceCapabilities.isNotEmpty()) {
            add { ContextCapabilitiesEntry(context = context) }
        }
    }

@Composable
private fun ContextPermissionRow(permission: WorkspacePermissionLevel) {
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

@Composable
private fun ContextCapabilitiesEntry(context: SessionRuntimeContext) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.session_context_field_capabilities),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WorkspaceCapabilityChips(capabilities = context.workspaceCapabilities)
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

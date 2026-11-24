package com.everfold.feature.sessions.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.ModelCapability
import com.everfold.core.model.NodeStatus
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.WorkspaceCapability
import com.everfold.core.model.WorkspacePermissionLevel
import com.everfold.feature.sessions.R
import kotlinx.collections.immutable.ImmutableSet

@StringRes
internal fun WorkspacePermissionLevel.labelRes(): Int =
    when (this) {
        WorkspacePermissionLevel.READ_ONLY -> R.string.workspace_perm_read_only
        WorkspacePermissionLevel.WORKSPACE_WRITE -> R.string.workspace_perm_write
        WorkspacePermissionLevel.FULL_ACCESS -> R.string.workspace_perm_full
    }

@StringRes
internal fun WorkspaceCapability.labelRes(): Int =
    when (this) {
        WorkspaceCapability.SHELL -> R.string.workspace_cap_shell
        WorkspaceCapability.FILE_EDIT -> R.string.workspace_cap_file_edit
        WorkspaceCapability.MCP -> R.string.workspace_cap_mcp
        WorkspaceCapability.CODE_MODE -> R.string.workspace_cap_code_mode
        WorkspaceCapability.NETWORK -> R.string.workspace_cap_network
    }

@StringRes
internal fun ReasoningEffort.labelRes(): Int =
    when (this) {
        ReasoningEffort.LOW -> R.string.effort_low
        ReasoningEffort.MEDIUM -> R.string.effort_medium
        ReasoningEffort.HIGH -> R.string.effort_high
        ReasoningEffort.MAX -> R.string.effort_max
    }

@Composable
internal fun WorkspacePermissionLevel.badgeColors(): Pair<Color, Color> =
    when (this) {
        WorkspacePermissionLevel.READ_ONLY -> {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }

        WorkspacePermissionLevel.WORKSPACE_WRITE -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }

        WorkspacePermissionLevel.FULL_ACCESS -> {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        }
    }

@Composable
internal fun NodeStatus.dotColor(): Color =
    when (this) {
        NodeStatus.ONLINE -> MaterialTheme.colorScheme.primary
        NodeStatus.DEGRADED -> MaterialTheme.colorScheme.tertiary
        NodeStatus.OFFLINE -> MaterialTheme.colorScheme.outline
    }

internal fun SessionRuntimeContext.compactSubtitle(): String {
    val node = nodeName.trim()
    val workspace = workspaceName.trim()
    return when {
        node.isNotEmpty() && workspace.isNotEmpty() -> "$node · $workspace"
        node.isNotEmpty() -> node
        else -> workspace
    }
}

@Composable
internal fun WorkspacePermissionBadge(
    permission: WorkspacePermissionLevel,
    modifier: Modifier = Modifier,
) {
    val (bg, fg) = permission.badgeColors()
    EverfoldStatusPill(
        text = stringResource(permission.labelRes()),
        containerColor = bg,
        contentColor = fg,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkspaceCapabilityChips(
    capabilities: ImmutableSet<WorkspaceCapability>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        capabilities.forEach { capability ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = stringResource(capability.labelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

internal fun ModelCapability.shortTag(): String =
    when (this) {
        ModelCapability.REASONING -> "Reasoning"
        ModelCapability.TOOL_USE -> "Tools"
        ModelCapability.VISION -> "Vision"
        ModelCapability.CODE_MODE -> "CodeMode"
    }

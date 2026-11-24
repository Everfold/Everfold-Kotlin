package com.everfold.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.effectsSpec
import com.everfold.core.designsystem.theme.spatialSpec
import com.everfold.core.model.ActionKind
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.ActionTrace
import com.everfold.core.model.AnimationSpeed
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionTraceGroupCard(
    actions: ImmutableList<ActionTrace>,
    expandWhenRunning: Boolean,
    expandWhenCompleted: Boolean,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false,
) {
    val isRunning = actions.any { it.status == ActionStatus.RUNNING }
    val defaultExpanded = if (isRunning) expandWhenRunning else expandWhenCompleted
    var manualOverride by rememberSaveable(isRunning) { mutableStateOf<Boolean?>(null) }
    val isExpanded = manualOverride ?: defaultExpanded
    val animationSpeed = LocalAnimationSpeed.current
    val spatialIntSizeSpec = animationSpeed.spatialSpec<IntSize>()
    val effectsFloatSpec = animationSpeed.effectsSpec<Float>()
    val enterTransition =
        if (animationSpeed == AnimationSpeed.OFF) {
            EnterTransition.None
        } else {
            expandVertically(spatialIntSizeSpec) + fadeIn(effectsFloatSpec)
        }
    val exitTransition =
        if (animationSpeed == AnimationSpeed.OFF) {
            ExitTransition.None
        } else {
            shrinkVertically(spatialIntSizeSpec) + fadeOut(effectsFloatSpec)
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ActionGroupHeader(
                actionCount = actions.size,
                isRunning = isRunning,
                isPaused = isPaused,
                expanded = isExpanded,
                onToggle = { manualOverride = !isExpanded },
            )
            if (isRunning && !isPaused) {
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
                )
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = enterTransition,
                exit = exitTransition,
            ) {
                EverfoldGroupedList(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp),
                ) {
                    actions.forEachIndexed { index, trace ->
                        EverfoldGroupedItem(
                            index = index,
                            count = actions.size,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            ActionTraceRow(trace = trace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionGroupHeader(
    actionCount: Int,
    isRunning: Boolean,
    isPaused: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ActionGroupStatusIcon(isRunning = isRunning, isPaused = isPaused)
            Text(
                text =
                    when {
                        isRunning && isPaused -> stringResource(R.string.action_group_paused, actionCount)
                        isRunning -> stringResource(R.string.action_group_running, actionCount)
                        else -> stringResource(R.string.action_group_completed, actionCount)
                    },
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ActionGroupStatusIcon(
    isRunning: Boolean,
    isPaused: Boolean,
) {
    when {
        isRunning && isPaused -> {
            Icon(
                imageVector = Icons.Rounded.PauseCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp),
            )
        }

        isRunning -> {
            LoadingIndicator(modifier = Modifier.size(20.dp))
        }

        else -> {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ActionTraceRow(trace: ActionTrace) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = trace.kind.toIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 2.dp).size(18.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = trace.title,
                    style = MaterialTheme.typography.labelLarge,
                )
                trace.durationMs?.let { ms ->
                    Text(
                        text = "${ms}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = trace.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            trace.executedOn?.let { node ->
                EverfoldStatusPill(
                    text = "${node.nodeName} · ${node.platform}",
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trace.status == ActionStatus.FAILED) {
            Icon(
                imageVector = Icons.Rounded.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun ActionKind.toIcon(): ImageVector =
    when (this) {
        ActionKind.LLM_REQUEST -> Icons.Rounded.AutoAwesome
        ActionKind.TOOL_CALL -> Icons.Rounded.Build
        ActionKind.MCP_CALL -> Icons.Rounded.Hub
        ActionKind.CODE_MODE -> Icons.Rounded.Code
    }

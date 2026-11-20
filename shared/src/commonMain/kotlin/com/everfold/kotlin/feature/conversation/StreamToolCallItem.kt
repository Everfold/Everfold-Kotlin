package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.stream_resumed_tag
import everfold.shared.generated.resources.stream_seconds_short
import everfold.shared.generated.resources.stream_toggle_tool_logs

private const val MILLIS_PER_SEC = 1000L
private const val LIVE_LOG_TAIL_LINES = 5
private const val CHEVRON_OPEN_DEG = 180f

@Composable
internal fun StreamToolCallBlock(
    block: AgentStreamBlock.ToolCall,
    modifier: Modifier = Modifier,
) {
    var manualExpanded by rememberSaveable(block.id) { mutableStateOf<Boolean?>(null) }
    val autoOpen = block.status != StreamRunStatus.Completed
    val expanded = manualExpanded ?: autoOpen
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ToolCallHeaderRow(
            block = block,
            expanded = expanded,
            onToggle = { manualExpanded = !expanded },
        )
        if (expanded && (block.command.isNotBlank() || block.logLines.isNotEmpty())) {
            ToolCallConsoleOutput(
                block = block,
                showFullLog = manualExpanded == true || block.status != StreamRunStatus.Running,
            )
        }
    }
}

@Composable
private fun ToolCallHeaderRow(
    block: AgentStreamBlock.ToolCall,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val toggleDesc = localizedString(Res.string.stream_toggle_tool_logs)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { contentDescription = toggleDesc }
                .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToolStatusIcon(block.status)
        Text(
            block.toolName,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            block.summary.resolve(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color =
                if (block.status == StreamRunStatus.Failed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        if (block.resumed) {
            Text(
                localizedString(Res.string.stream_resumed_tag),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (block.durationMillis > 0L && block.status == StreamRunStatus.Completed) {
            val sec = (block.durationMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
            Text(
                localizedString(Res.string.stream_seconds_short, sec),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppIcon(AppSymbol.ChevronDown, Modifier.size(14.dp).rotate(if (expanded) CHEVRON_OPEN_DEG else 0f))
    }
}

@Composable
private fun ToolStatusIcon(status: StreamRunStatus) {
    when (status) {
        StreamRunStatus.Running -> {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
        }

        StreamRunStatus.Completed -> {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                AppIcon(AppSymbol.Check, Modifier.size(14.dp))
            }
        }

        StreamRunStatus.Paused -> {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                AppIcon(AppSymbol.Stop, Modifier.size(12.dp))
            }
        }

        StreamRunStatus.Failed -> {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                AppIcon(AppSymbol.Close, Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun ToolCallConsoleOutput(
    block: AgentStreamBlock.ToolCall,
    showFullLog: Boolean,
) {
    val displayedLines = if (showFullLog) block.logLines else block.logLines.takeLast(LIVE_LOG_TAIL_LINES)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (block.command.isNotBlank()) {
                Text(
                    "$ ${block.command}",
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            block.progress?.takeIf { block.status == StreamRunStatus.Running }?.let { fraction ->
                LinearProgressIndicator(
                    progress = { fraction.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                )
            }
            if (displayedLines.isNotEmpty()) {
                Text(
                    displayedLines.joinToString("\n"),
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

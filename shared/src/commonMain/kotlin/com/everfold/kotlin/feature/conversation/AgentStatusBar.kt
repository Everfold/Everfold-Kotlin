package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.agent_completed
import everfold.shared.generated.resources.agent_continue
import everfold.shared.generated.resources.agent_failed
import everfold.shared.generated.resources.agent_idle
import everfold.shared.generated.resources.agent_retry
import everfold.shared.generated.resources.agent_running
import everfold.shared.generated.resources.agent_step_prepare
import everfold.shared.generated.resources.agent_step_reply
import everfold.shared.generated.resources.agent_stopped
import everfold.shared.generated.resources.agent_waiting
import everfold.shared.generated.resources.agent_waiting_hint
import everfold.shared.generated.resources.execution_details
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val EXPANDED_CHEVRON_ROTATION = 180f

/** 回复的执行状态，详情默认折叠，异常状态提供恢复入口。 */
@Composable
internal fun AgentStatusBar(
    state: AgentRunState,
    onRetry: () -> Unit,
    canRetry: Boolean = true,
) {
    if (state == AgentRunState.Idle) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusSummary(state, expanded) { expanded = !expanded }
            Spacer(Modifier.weight(1f))
            if (state == AgentRunState.Waiting || state == AgentRunState.Failed || state == AgentRunState.Stopped) {
                TextButton(
                    text = localizedString(if (state == AgentRunState.Failed) Res.string.agent_retry else Res.string.agent_continue),
                    onClick = onRetry,
                    enabled = canRetry,
                    colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                    insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        if (state == AgentRunState.Waiting) {
            Text(
                text = localizedString(Res.string.agent_waiting_hint),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (expanded) {
            Text(
                text = "1. " + localizedString(Res.string.agent_step_prepare),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "2. " + localizedString(Res.string.agent_step_reply) + " · " + localizedString(state.label),
                modifier = Modifier.padding(bottom = 8.dp),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun StatusSummary(
    state: AgentRunState,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val detailsLabel = localizedString(Res.string.execution_details)
    Row(
        modifier =
            Modifier
                .squircleClip(8.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = detailsLabel }
                .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (state == AgentRunState.Running) {
            InfiniteProgressIndicator(
                modifier = Modifier.progressSemantics(),
                color = MiuixTheme.colorScheme.primary,
                size = 12.dp,
                strokeWidth = 1.5.dp,
            )
        }
        Text(
            text = localizedString(state.label),
            style = MiuixTheme.textStyles.footnote1,
            color =
                if (state == AgentRunState.Failed) {
                    MiuixTheme.colorScheme.error
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
        )
        AppIcon(AppSymbol.ChevronDown, Modifier.size(16.dp).rotate(if (expanded) EXPANDED_CHEVRON_ROTATION else 0f))
    }
}

/** 会话列表状态栏和预览菜单共用的中文状态资源映射 */
internal val AgentRunState.label
    get() =
        when (this) {
            AgentRunState.Idle -> Res.string.agent_idle
            AgentRunState.Running -> Res.string.agent_running
            AgentRunState.Waiting -> Res.string.agent_waiting
            AgentRunState.Completed -> Res.string.agent_completed
            AgentRunState.Stopped -> Res.string.agent_stopped
            AgentRunState.Failed -> Res.string.agent_failed
        }

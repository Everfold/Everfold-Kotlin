package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.agent_completed
import everfold.shared.generated.resources.agent_continue
import everfold.shared.generated.resources.agent_failed
import everfold.shared.generated.resources.agent_failed_hint
import everfold.shared.generated.resources.agent_idle
import everfold.shared.generated.resources.agent_retry
import everfold.shared.generated.resources.agent_running
import everfold.shared.generated.resources.agent_step_prepare
import everfold.shared.generated.resources.agent_step_reply
import everfold.shared.generated.resources.agent_stopped
import everfold.shared.generated.resources.agent_waiting
import everfold.shared.generated.resources.agent_waiting_hint
import everfold.shared.generated.resources.execution_details

/** 显示执行状态进度和演示步骤并在等待失败或停止时提供恢复入口 */
@Composable
internal fun AgentStatusBar(
    state: AgentRunState,
    onRetry: () -> Unit,
) {
    if (state == AgentRunState.Idle) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                // 状态见下方 AgentRunState.label 映射
                localizedString(state.label),
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = if (state == AgentRunState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // 执行详情
            TextButton(onClick = { expanded = !expanded }) { Text(localizedString(Res.string.execution_details)) }
        }
        if (state == AgentRunState.Running) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state == AgentRunState.Waiting || state == AgentRunState.Failed) {
            Text(
                localizedString(
                    if (state ==
                        AgentRunState.Waiting
                    ) {
                        // 请补充要求或确认继续执行
                        Res.string.agent_waiting_hint
                    } else {
                        // 任务执行失败请重试
                        Res.string.agent_failed_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (state == AgentRunState.Waiting || state == AgentRunState.Failed || state == AgentRunState.Stopped) {
            TextButton(onClick = onRetry) {
                Text(
                    localizedString(
                        if (state ==
                            AgentRunState.Waiting || state == AgentRunState.Stopped
                        ) {
                            // 继续执行
                            Res.string.agent_continue
                        } else {
                            // 重试
                            Res.string.agent_retry
                        },
                    ),
                )
            }
        }
        if (expanded) {
            // 整理输入
            Text("1. " + localizedString(Res.string.agent_step_prepare), style = MaterialTheme.typography.bodySmall)
            Text(
                // 生成回复中文状态见下方 AgentRunState.label 映射
                "2. " + localizedString(Res.string.agent_step_reply) + " · " + localizedString(state.label),
                Modifier.padding(bottom = 12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** 会话列表状态栏和预览菜单共用的中文状态资源映射 */
internal val AgentRunState.label
    get() =
        when (this) {

            // 就绪
            AgentRunState.Idle -> Res.string.agent_idle

            // 执行中
            AgentRunState.Running -> Res.string.agent_running

            // 等待回复
            AgentRunState.Waiting -> Res.string.agent_waiting

            // 已完成
            AgentRunState.Completed -> Res.string.agent_completed

            // 已停止
            AgentRunState.Stopped -> Res.string.agent_stopped

            // 执行失败
            AgentRunState.Failed -> Res.string.agent_failed
        }

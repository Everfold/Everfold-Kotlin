package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.conversation.AgentRunState

internal fun publishBridgeSnapshot(
    bridge: PlatformLongTaskBridge,
    record: TaskRunRecord?,
    conversationId: String,
    runState: AgentRunState,
) {
    if (bridge === NoopLongTaskBridge) return
    if (record == null || runState == AgentRunState.Idle) {
        bridge.dismiss(conversationId)
    } else if (record.connectionId == BUILT_IN_CONNECTION_ID) {
        val english = isEnglishNotificationLocale()
        val kind = mapRunStateToNotificationKind(runState)
        val percent = record.progressPercent(runState)
        val toolName = record.activeToolName()
        bridge.publish(
            TaskNotificationPayload(
                conversationId = conversationId,
                title = resolveScenarioTitle(record.scenario.kind, english),
                body = formatNotificationBody(kind, toolName, percent, english),
                progressPercent = percent,
                kind = kind,
                actionLabel = resolveNotificationActionLabel(kind, english),
            ),
        )
    }
}

private fun mapRunStateToNotificationKind(runState: AgentRunState): TaskNotificationKind =
    when (runState) {
        AgentRunState.Running -> TaskNotificationKind.Running
        AgentRunState.Waiting -> TaskNotificationKind.Waiting
        AgentRunState.Stopped -> TaskNotificationKind.Paused
        AgentRunState.Failed -> TaskNotificationKind.Failed
        AgentRunState.Completed, AgentRunState.Idle -> TaskNotificationKind.Completed
    }

private fun resolveScenarioTitle(
    kind: ScenarioKind,
    english: Boolean,
): String =
    when (kind) {
        ScenarioKind.StorageArchive -> if (english) "Downloads & Screenshots archive" else "本机存储与下载目录深度归档"
        ScenarioKind.Diagnostics -> if (english) "Battery drain & crash diagnostics" else "本机后台耗电与崩溃日志诊断"
        ScenarioKind.DocumentIndex -> if (english) "Offline document index build" else "本地工作区文档离线索引构建"
        ScenarioKind.GenericWorkspace -> if (english) "On-device multi-stage task" else "本机多阶段任务分析与执行"
    }

private fun formatNotificationBody(
    kind: TaskNotificationKind,
    toolName: String,
    percent: Int,
    english: Boolean,
): String =
    when (kind) {
        TaskNotificationKind.Completed -> {
            if (english) "Task completed" else "任务已完成"
        }

        TaskNotificationKind.Waiting -> {
            if (english) "Awaiting confirmation ($percent%)" else "等待确认处理方式 ($percent%)"
        }

        TaskNotificationKind.Paused -> {
            if (english) "Paused · checkpoint saved ($percent%)" else "已暂停 · 进度断点已保存 ($percent%)"
        }

        TaskNotificationKind.Failed -> {
            if (english) "Interrupted ($percent%) · tap Retry" else "执行中断 ($percent%) · 可断点重试"
        }

        TaskNotificationKind.Running -> {
            if (toolName.isNotBlank()) {
                if (english) "Running $toolName ($percent%)" else "正在执行 $toolName ($percent%)"
            } else {
                if (english) "Streaming analysis ($percent%)" else "正在分析与输出 ($percent%)"
            }
        }
    }

private fun resolveNotificationActionLabel(
    kind: TaskNotificationKind,
    english: Boolean,
): String? =
    when (kind) {
        TaskNotificationKind.Running -> if (english) "Pause" else "暂停"
        TaskNotificationKind.Failed -> if (english) "Retry" else "重试"
        TaskNotificationKind.Waiting, TaskNotificationKind.Paused -> if (english) "Continue" else "继续执行"
        TaskNotificationKind.Completed -> null
    }

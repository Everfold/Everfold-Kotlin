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
        val stepNumber = record.currentStepIndex.coerceIn(0, record.scenario.steps.lastIndex) + 1
        val totalSteps = record.scenario.steps.size
        val stepTool =
            record.scenario.steps
                .getOrNull(stepNumber - 1)
                ?.toolName
                .orEmpty()
        bridge.publish(
            TaskNotificationPayload(
                conversationId = conversationId,
                title = resolveScenarioTitle(record.scenario.kind, english),
                body = formatNotificationBody(kind, stepNumber, totalSteps, stepTool, percent, english),
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
    stepNumber: Int,
    totalSteps: Int,
    stepTool: String,
    percent: Int,
    english: Boolean,
): String {
    val toolSuffix = if (stepTool.isBlank()) "" else " · $stepTool"
    return when (kind) {
        TaskNotificationKind.Completed -> {
            if (english) "Completed all $totalSteps steps" else "已完成全部 $totalSteps 个步骤"
        }

        TaskNotificationKind.Waiting -> {
            if (english) "Step $stepNumber/$totalSteps awaiting confirmation" else "第 $stepNumber/$totalSteps 步等待确认"
        }

        TaskNotificationKind.Paused -> {
            if (english) "Paused at step $stepNumber/$totalSteps ($percent%)" else "已暂停在第 $stepNumber/$totalSteps 步 ($percent%)"
        }

        TaskNotificationKind.Failed -> {
            if (english) "Interrupted at step $stepNumber/$totalSteps ($percent%)" else "中断于第 $stepNumber/$totalSteps 步 ($percent%)"
        }

        TaskNotificationKind.Running -> {
            if (english) "Step $stepNumber/$totalSteps$toolSuffix ($percent%)" else "第 $stepNumber/$totalSteps 步$toolSuffix ($percent%)"
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

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
        val segments = record.buildStageSegments(runState)
        bridge.publish(
            TaskNotificationPayload(
                conversationId = conversationId,
                title = resolveScenarioTitle(record.scenario.kind, english),
                body = formatNotificationBody(kind, toolName, percent, english),
                progressPercent = percent,
                kind = kind,
                actionLabel = resolveNotificationActionLabel(kind, english),
                stageSegments = segments,
                gatePointPercent = record.resolveGatePointPercent(segments),
                activeStageKind = record.resolveActiveStageKind(),
            ),
        )
    }
}

private fun TaskRunRecord.buildStageSegments(runState: AgentRunState): List<TaskStageSegment> {
    val stages = scenario.stages
    if (stages.isEmpty()) return emptyList()
    val baseWeight = PERCENT_HUNDRED / stages.size
    val remainder = PERCENT_HUNDRED % stages.size
    val allReached = runState == AgentRunState.Completed
    return stages.mapIndexed { index, stage ->
        val weight = if (index < remainder) baseWeight + 1 else baseWeight
        val reached = allReached || index <= currentStageIndex
        TaskStageSegment(
            weight = weight,
            kind = stage.toSegmentKind(),
            reached = reached,
        )
    }
}

private fun TaskRunRecord.resolveGatePointPercent(segments: List<TaskStageSegment>): Int? {
    val gateIndex = scenario.stages.indexOfFirst { it is StreamStageSpec.ApprovalStage }
    if (gateIndex < 0) return null
    val cumulative = segments.take(gateIndex).sumOf { it.weight }
    return cumulative.coerceIn(1, PERCENT_HUNDRED - 1)
}

private fun TaskRunRecord.resolveActiveStageKind(): TaskStageSegmentKind =
    scenario.stages
        .getOrNull(currentStageIndex.coerceIn(0, (scenario.stages.size - 1).coerceAtLeast(0)))
        ?.toSegmentKind()
        ?: TaskStageSegmentKind.Llm

private fun StreamStageSpec.toSegmentKind(): TaskStageSegmentKind =
    when (this) {
        is StreamStageSpec.ThinkStage, is StreamStageSpec.NarrationStage -> TaskStageSegmentKind.Llm
        is StreamStageSpec.ToolStage -> TaskStageSegmentKind.Tool
        is StreamStageSpec.ApprovalStage -> TaskStageSegmentKind.Gate
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

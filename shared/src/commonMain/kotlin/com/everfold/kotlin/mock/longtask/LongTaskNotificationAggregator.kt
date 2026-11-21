package com.everfold.kotlin.mock.longtask

internal data class AggregatedNotificationUpdate(
    val primaryOngoing: TaskNotificationPayload?,
    val completedEvent: TaskNotificationPayload? = null,
)

/**
 * 多会话并发长程任务的流体云胶囊聚合器。
 *
 * - 维护所有未结束会话的实时快照；
 * - 按 `Waiting > Failed > Running > Paused` 优先级选出唯一主胶囊；
 * - 同优先级（如多个会话同时 Running）下保持主会话黏性，避免状态栏胶囊高频闪烁切换；
 * - 当前主会话完成时独立发出完成通知，并无缝将主胶囊接力给仍在运行的其余会话。
 */
internal class LongTaskNotificationAggregator {
    private val activeTasks = linkedMapOf<String, TaskNotificationPayload>()
    private var stickyPrimaryId: String? = null

    fun onPublish(
        payload: TaskNotificationPayload,
        english: Boolean,
    ): AggregatedNotificationUpdate {
        if (payload.kind == TaskNotificationKind.Completed) {
            activeTasks.remove(payload.conversationId)
            if (stickyPrimaryId == payload.conversationId) {
                stickyPrimaryId = null
            }
            return AggregatedNotificationUpdate(
                primaryOngoing = selectPrimary(english),
                completedEvent = payload,
            )
        }
        activeTasks[payload.conversationId] = payload
        return AggregatedNotificationUpdate(
            primaryOngoing = selectPrimary(english),
            completedEvent = null,
        )
    }

    fun onDismiss(
        conversationId: String,
        english: Boolean,
    ): AggregatedNotificationUpdate {
        activeTasks.remove(conversationId)
        if (stickyPrimaryId == conversationId) {
            stickyPrimaryId = null
        }
        return AggregatedNotificationUpdate(
            primaryOngoing = selectPrimary(english),
            completedEvent = null,
        )
    }

    private fun selectPrimary(english: Boolean): TaskNotificationPayload? {
        if (activeTasks.isEmpty()) {
            stickyPrimaryId = null
            return null
        }
        val bestRank = activeTasks.values.minOf { priorityRank(it.kind) }
        val stickyCandidate =
            stickyPrimaryId
                ?.let { activeTasks[it] }
                ?.takeIf { priorityRank(it.kind) == bestRank }
        val chosen =
            stickyCandidate
                ?: activeTasks.values.last { priorityRank(it.kind) == bestRank }
        stickyPrimaryId = chosen.conversationId

        val activeCount =
            activeTasks.values
                .count { it.kind == TaskNotificationKind.Running || it.kind == TaskNotificationKind.Waiting }
                .coerceAtLeast(1)
        val otherCount = (activeTasks.size - 1).coerceAtLeast(0)
        return chosen.copy(
            concurrentActiveCount = activeCount,
            concurrentSummary = formatConcurrentSummary(otherCount, english),
            shortChipLabel = formatShortChipLabel(chosen, activeCount, english),
        )
    }

    private fun priorityRank(kind: TaskNotificationKind): Int =
        when (kind) {
            TaskNotificationKind.Waiting -> PRIORITY_WAITING
            TaskNotificationKind.Failed -> PRIORITY_FAILED
            TaskNotificationKind.Running -> PRIORITY_RUNNING
            TaskNotificationKind.Paused -> PRIORITY_PAUSED
            TaskNotificationKind.Completed -> PRIORITY_COMPLETED
        }

    private fun formatConcurrentSummary(
        otherCount: Int,
        english: Boolean,
    ): String? {
        if (otherCount <= 0) return null
        return if (english) {
            "+$otherCount other session task(s)"
        } else {
            "另有 $otherCount 个会话任务"
        }
    }

    private fun formatShortChipLabel(
        primary: TaskNotificationPayload,
        activeCount: Int,
        english: Boolean,
    ): String {
        val clamped = primary.progressPercent.coerceIn(0, PERCENT_HUNDRED)
        return when (primary.kind) {
            TaskNotificationKind.Waiting -> if (english) "Confirm" else "待确认"
            TaskNotificationKind.Failed -> if (english) "Failed" else "已中断"
            TaskNotificationKind.Paused -> if (english) "Paused" else "已暂停"
            TaskNotificationKind.Running -> if (activeCount > 1) "$activeCount·$clamped%" else "$clamped%"
            TaskNotificationKind.Completed -> ""
        }
    }

    private companion object {
        const val PRIORITY_WAITING = 0
        const val PRIORITY_FAILED = 1
        const val PRIORITY_RUNNING = 2
        const val PRIORITY_PAUSED = 3
        const val PRIORITY_COMPLETED = 4
    }
}

package com.everfold.core.liveactivity

import com.everfold.core.model.ActionStatus
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import com.everfold.core.model.NotificationPreferences
import com.everfold.core.model.ObservedItem

class LiveActivityStateReducer {
    private val previousStates = mutableMapOf<String, LoopState>()
    private var retainedTerminalSessionId: String? = null

    fun reduce(
        sessions: List<AgentSessionSummary>,
        primaryDetail: AgentSessionDetail?,
        isForeground: Boolean,
        prefs: NotificationPreferences,
    ): LiveActivityRenderPlan {
        val activeSessions = sessions.filter { !it.isArchived && it.loopState.isActivePhase() }
        val alerts =
            if (isForeground) {
                retainedTerminalSessionId = null
                emptyList()
            } else {
                collectTransitionAlerts(
                    sessions = sessions,
                    previousStates = previousStates,
                    prefs = prefs,
                    onRetainTerminal = { retainedTerminalSessionId = it },
                )
            }
        syncPreviousStates(sessions = sessions, previousStates = previousStates)

        val capsule =
            if (isForeground || !prefs.liveActivityEnabled) {
                null
            } else {
                resolveBackgroundCapsule(
                    sessions = sessions,
                    activeSessions = activeSessions,
                    primaryDetail = primaryDetail,
                    retainedTerminalSessionId = retainedTerminalSessionId,
                )
            }
        return LiveActivityRenderPlan(capsule = capsule, alerts = alerts)
    }

    fun markSessionOpened(sessionId: String) {
        if (retainedTerminalSessionId == sessionId) {
            retainedTerminalSessionId = null
        }
    }

    fun selectPrimarySessionId(sessions: List<AgentSessionSummary>): String? {
        val activeWinner =
            sessions
                .filter { !it.isArchived && it.loopState.isActivePhase() }
                .minByOrNull { it.loopState.priorityRank() }
                ?.id
        val retainedWinner =
            retainedTerminalSessionId?.takeIf { id ->
                sessions.any { it.id == id && !it.isArchived && it.loopState.isTerminalPhase() }
            }
        return activeWinner ?: retainedWinner
    }
}

private fun resolveBackgroundCapsule(
    sessions: List<AgentSessionSummary>,
    activeSessions: List<AgentSessionSummary>,
    primaryDetail: AgentSessionDetail?,
    retainedTerminalSessionId: String?,
): LiveActivitySnapshot? {
    val activePrimary = activeSessions.minByOrNull { it.loopState.priorityRank() }
    val target =
        activePrimary
            ?: retainedTerminalSessionId?.let { id ->
                sessions.firstOrNull { it.id == id && !it.isArchived && it.loopState.isTerminalPhase() }
            }
            ?: return null
    val detail = primaryDetail?.takeIf { it.summary.id == target.id }
    val otherActiveCount = activeSessions.count { it.id != target.id }
    return target.toSnapshot(detail = detail, otherActiveCount = otherActiveCount)
}

private fun collectTransitionAlerts(
    sessions: List<AgentSessionSummary>,
    previousStates: Map<String, LoopState>,
    prefs: NotificationPreferences,
    onRetainTerminal: (String) -> Unit,
): List<LiveActivitySnapshot> {
    val alerts = mutableListOf<LiveActivitySnapshot>()
    sessions.forEach { summary ->
        val prev = previousStates[summary.id]
        val transitionedFromActive = prev != null && prev.isActivePhase() && summary.loopState.isTerminalPhase()
        if (transitionedFromActive && !summary.isArchived) {
            onRetainTerminal(summary.id)
            if (shouldAlert(summary.loopState, prefs)) {
                alerts += summary.toSnapshot(detail = null, otherActiveCount = 0)
            }
        }
    }
    return alerts
}

private fun syncPreviousStates(
    sessions: List<AgentSessionSummary>,
    previousStates: MutableMap<String, LoopState>,
) {
    previousStates.clear()
    sessions.forEach { previousStates[it.id] = it.loopState }
}

private fun shouldAlert(
    state: LoopState,
    prefs: NotificationPreferences,
): Boolean =
    when (state) {
        LoopState.AWAITING_INPUT, LoopState.FAILED -> prefs.alertOnNeedsAttention
        LoopState.COMPLETED -> prefs.alertOnCompleted
        else -> false
    }

private fun AgentSessionSummary.toSnapshot(
    detail: AgentSessionDetail?,
    otherActiveCount: Int,
): LiveActivitySnapshot {
    val phase = loopState.toLivePhase()
    val latestGroup = detail?.items?.filterIsInstance<ObservedItem.ActionGroup>()?.lastOrNull()
    val actions = latestGroup?.actions.orEmpty()
    val completedSteps = actions.count { it.status == ActionStatus.SUCCEEDED }
    val totalSteps = if (actions.isNotEmpty()) actions.size else 0
    val currentActionTitle =
        actions.lastOrNull { it.status == ActionStatus.RUNNING }?.title
            ?: actions.lastOrNull()?.title
    val detailText = currentActionTitle ?: lastMessagePreview.trim()
    return LiveActivitySnapshot(
        sessionId = id,
        sessionTitle = title,
        phase = phase,
        hostNodeName = loopHostNodeName,
        workspaceName = workspaceName,
        statusDetail = detailText,
        completedSteps = completedSteps,
        totalSteps = totalSteps,
        otherActiveCount = otherActiveCount,
    )
}

private fun LoopState.isActivePhase(): Boolean =
    this == LoopState.PLANNING || this == LoopState.EXECUTING_ACTION || this == LoopState.PAUSED

private fun LoopState.isTerminalPhase(): Boolean =
    this == LoopState.AWAITING_INPUT || this == LoopState.COMPLETED || this == LoopState.FAILED

private fun LoopState.priorityRank(): Int =
    when (this) {
        LoopState.EXECUTING_ACTION -> 0
        LoopState.PLANNING -> 1
        LoopState.PAUSED -> 2
        else -> Int.MAX_VALUE
    }

private fun LoopState.toLivePhase(): LiveActivityPhase =
    when (this) {
        LoopState.PLANNING -> LiveActivityPhase.PLANNING
        LoopState.EXECUTING_ACTION -> LiveActivityPhase.EXECUTING
        LoopState.PAUSED -> LiveActivityPhase.PAUSED
        LoopState.AWAITING_INPUT -> LiveActivityPhase.AWAITING_INPUT
        LoopState.COMPLETED -> LiveActivityPhase.COMPLETED
        LoopState.FAILED -> LiveActivityPhase.FAILED
        LoopState.IDLE -> LiveActivityPhase.COMPLETED
    }

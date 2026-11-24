package com.everfold.core.liveactivity

import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import com.everfold.core.model.NotificationPreferences

class LiveActivityStateReducer {
    private val previousStates = mutableMapOf<String, LoopState>()
    private val postedSessionIds = mutableSetOf<String>()
    private val activeCapsuleSessionIds = mutableSetOf<String>()

    fun reduce(
        sessions: List<AgentSessionSummary>,
        isForeground: Boolean,
        prefs: NotificationPreferences,
    ): LiveActivityDispatchPlan {
        if (isForeground) {
            val dismissed = activeCapsuleSessionIds.toSet()
            activeCapsuleSessionIds.clear()
            postedSessionIds.removeAll(dismissed)
            syncPreviousStates(sessions = sessions, previousStates = previousStates)
            return LiveActivityDispatchPlan(
                eventsToPost = emptyList(),
                sessionIdsToDismiss = dismissed,
            )
        }

        val sessionById = sessions.associateBy { it.id }
        val toDismiss =
            postedSessionIds
                .filter { id ->
                    val current = sessionById[id]
                    current == null || current.isArchived || !current.loopState.isTerminalPhase()
                }.toMutableSet()
        postedSessionIds.removeAll(toDismiss)
        activeCapsuleSessionIds.removeAll(toDismiss)

        val eventsToPost =
            collectTransitionEvents(
                sessions = sessions,
                previousStates = previousStates,
                prefs = prefs,
            )
        eventsToPost.forEach { event ->
            postedSessionIds.add(event.sessionId)
            if (event.promoteAsCapsule) {
                activeCapsuleSessionIds.add(event.sessionId)
            } else {
                activeCapsuleSessionIds.remove(event.sessionId)
            }
        }
        syncPreviousStates(sessions = sessions, previousStates = previousStates)
        return LiveActivityDispatchPlan(eventsToPost = eventsToPost, sessionIdsToDismiss = toDismiss)
    }

    fun markSessionOpened(sessionId: String) {
        postedSessionIds.remove(sessionId)
        activeCapsuleSessionIds.remove(sessionId)
    }
}

private fun collectTransitionEvents(
    sessions: List<AgentSessionSummary>,
    previousStates: Map<String, LoopState>,
    prefs: NotificationPreferences,
): List<LiveActivityEvent> {
    val events = mutableListOf<LiveActivityEvent>()
    sessions.forEach { summary ->
        val prev = previousStates[summary.id]
        val transitionedFromActive = prev != null && prev.isActivePhase() && summary.loopState.isTerminalPhase()
        if (transitionedFromActive && !summary.isArchived && shouldNotify(summary.loopState, prefs)) {
            val kind = summary.loopState.toEventKind()
            events +=
                LiveActivityEvent(
                    sessionId = summary.id,
                    sessionTitle = summary.title,
                    kind = kind,
                    hostNodeName = summary.loopHostNodeName,
                    workspaceName = summary.workspaceName,
                    detailText = summary.lastMessagePreview.trim(),
                    promoteAsCapsule = kind.requiresAttention && prefs.liveActivityEnabled,
                )
        }
    }
    return events
}

private fun syncPreviousStates(
    sessions: List<AgentSessionSummary>,
    previousStates: MutableMap<String, LoopState>,
) {
    previousStates.clear()
    sessions.forEach { previousStates[it.id] = it.loopState }
}

private fun shouldNotify(
    state: LoopState,
    prefs: NotificationPreferences,
): Boolean =
    when (state) {
        LoopState.AWAITING_INPUT, LoopState.FAILED -> prefs.alertOnNeedsAttention
        LoopState.COMPLETED -> prefs.alertOnCompleted
        else -> false
    }

private fun LoopState.isActivePhase(): Boolean =
    this == LoopState.PLANNING || this == LoopState.EXECUTING_ACTION || this == LoopState.PAUSED

private fun LoopState.isTerminalPhase(): Boolean =
    this == LoopState.AWAITING_INPUT || this == LoopState.COMPLETED || this == LoopState.FAILED

private fun LoopState.toEventKind(): LiveActivityEventKind =
    when (this) {
        LoopState.AWAITING_INPUT -> LiveActivityEventKind.AWAITING_INPUT
        LoopState.FAILED -> LiveActivityEventKind.FAILED
        else -> LiveActivityEventKind.COMPLETED
    }

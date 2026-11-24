package com.everfold.core.liveactivity

import android.content.Intent

enum class LiveActivityEventKind {
    AWAITING_INPUT,
    FAILED,
    COMPLETED,
    ;

    val requiresAttention: Boolean
        get() = this == AWAITING_INPUT || this == FAILED
}

data class LiveActivityEvent(
    val sessionId: String,
    val sessionTitle: String,
    val kind: LiveActivityEventKind,
    val hostNodeName: String,
    val workspaceName: String,
    val detailText: String,
    val promoteAsCapsule: Boolean,
)

data class LiveActivityDispatchPlan(
    val eventsToPost: List<LiveActivityEvent>,
    val sessionIdsToDismiss: Set<String>,
)

interface LiveActivityPublisher {
    fun publishEvent(event: LiveActivityEvent)

    fun dismissSession(sessionId: String)
}

fun interface LiveActivityLaunchIntentFactory {
    fun createOpenSessionIntent(sessionId: String): Intent
}

object LiveActivityIntentContract {
    const val EXTRA_OPEN_SESSION_ID = "com.everfold.extra.OPEN_SESSION_ID"
}

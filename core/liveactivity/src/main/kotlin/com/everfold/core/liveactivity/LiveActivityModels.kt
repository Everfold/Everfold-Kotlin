package com.everfold.core.liveactivity

import android.content.Intent

enum class LiveActivityPhase {
    PLANNING,
    EXECUTING,
    PAUSED,
    AWAITING_INPUT,
    COMPLETED,
    FAILED,
    ;

    val isActive: Boolean
        get() = this == PLANNING || this == EXECUTING || this == PAUSED

    val isTerminal: Boolean
        get() = this == AWAITING_INPUT || this == COMPLETED || this == FAILED
}

data class LiveActivitySnapshot(
    val sessionId: String,
    val sessionTitle: String,
    val phase: LiveActivityPhase,
    val hostNodeName: String,
    val workspaceName: String,
    val statusDetail: String,
    val completedSteps: Int,
    val totalSteps: Int,
    val otherActiveCount: Int,
)

data class LiveActivityRenderPlan(
    val capsule: LiveActivitySnapshot?,
    val alerts: List<LiveActivitySnapshot>,
)

interface LiveActivityPublisher {
    fun publishCapsule(snapshot: LiveActivitySnapshot)

    fun dismissCapsule()

    fun publishAlert(snapshot: LiveActivitySnapshot)

    fun dismissAlert(sessionId: String)
}

fun interface LiveActivityLaunchIntentFactory {
    fun createOpenSessionIntent(sessionId: String): Intent
}

object LiveActivityIntentContract {
    const val EXTRA_OPEN_SESSION_ID = "com.everfold.extra.OPEN_SESSION_ID"
}

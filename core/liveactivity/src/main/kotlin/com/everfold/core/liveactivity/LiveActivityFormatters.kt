package com.everfold.core.liveactivity

import android.content.Context

private const val PROGRESS_MAX = 100
private const val PROGRESS_PLANNING_START = 18
private const val PROGRESS_STEP_SPAN = 64
private const val PROGRESS_MIN_PERCENT = 20
private const val PROGRESS_MAX_ACTIVE = 90
private const val PROGRESS_TERMINAL_FAILURE = 85

internal fun formatShortChipText(
    context: Context,
    snapshot: LiveActivitySnapshot,
): String =
    when (snapshot.phase) {
        LiveActivityPhase.PLANNING -> {
            context.getString(R.string.live_chip_planning)
        }

        LiveActivityPhase.EXECUTING -> {
            if (snapshot.totalSteps > 0) {
                val activeStep = (snapshot.completedSteps + 1).coerceAtMost(snapshot.totalSteps)
                context.getString(R.string.live_chip_step, activeStep, snapshot.totalSteps)
            } else {
                context.getString(R.string.live_chip_running)
            }
        }

        LiveActivityPhase.PAUSED -> {
            context.getString(R.string.live_chip_paused)
        }

        LiveActivityPhase.AWAITING_INPUT -> {
            context.getString(R.string.live_chip_attention)
        }

        LiveActivityPhase.FAILED -> {
            context.getString(R.string.live_chip_failed)
        }

        LiveActivityPhase.COMPLETED -> {
            context.getString(R.string.live_chip_completed)
        }
    }

internal fun formatLiveBodyText(
    context: Context,
    snapshot: LiveActivitySnapshot,
): String {
    val base =
        snapshot.statusDetail.ifBlank {
            when (snapshot.phase) {
                LiveActivityPhase.PLANNING -> context.getString(R.string.live_status_planning)
                LiveActivityPhase.EXECUTING -> context.getString(R.string.live_chip_running)
                LiveActivityPhase.PAUSED -> context.getString(R.string.live_status_paused)
                LiveActivityPhase.AWAITING_INPUT -> context.getString(R.string.live_status_attention)
                LiveActivityPhase.FAILED -> context.getString(R.string.live_status_failed)
                LiveActivityPhase.COMPLETED -> context.getString(R.string.live_status_completed)
            }
        }
    return if (snapshot.otherActiveCount > 0) {
        context.getString(R.string.live_extra_running, base, snapshot.otherActiveCount)
    } else {
        base
    }
}

internal fun formatAlertTitle(
    context: Context,
    snapshot: LiveActivitySnapshot,
): String =
    when (snapshot.phase) {
        LiveActivityPhase.AWAITING_INPUT -> context.getString(R.string.live_alert_title_attention, snapshot.sessionTitle)
        LiveActivityPhase.FAILED -> context.getString(R.string.live_alert_title_failed, snapshot.sessionTitle)
        else -> context.getString(R.string.live_alert_title_completed, snapshot.sessionTitle)
    }

internal fun formatContextSubText(snapshot: LiveActivitySnapshot): String {
    val host = snapshot.hostNodeName.trim()
    val workspace = snapshot.workspaceName.trim()
    return when {
        host.isNotEmpty() && workspace.isNotEmpty() -> "$host · $workspace"
        host.isNotEmpty() -> host
        else -> workspace
    }
}

internal fun computeProgressPercent(snapshot: LiveActivitySnapshot): Int =
    when (snapshot.phase) {
        LiveActivityPhase.PLANNING -> {
            PROGRESS_PLANNING_START
        }

        LiveActivityPhase.EXECUTING, LiveActivityPhase.PAUSED -> {
            if (snapshot.totalSteps <= 0) {
                PROGRESS_MIN_PERCENT
            } else {
                val ratio = snapshot.completedSteps.toFloat() / snapshot.totalSteps.toFloat()
                (PROGRESS_PLANNING_START + (ratio * PROGRESS_STEP_SPAN).toInt())
                    .coerceIn(PROGRESS_MIN_PERCENT, PROGRESS_MAX_ACTIVE)
            }
        }

        LiveActivityPhase.AWAITING_INPUT, LiveActivityPhase.FAILED -> {
            PROGRESS_TERMINAL_FAILURE
        }

        LiveActivityPhase.COMPLETED -> {
            PROGRESS_MAX
        }
    }

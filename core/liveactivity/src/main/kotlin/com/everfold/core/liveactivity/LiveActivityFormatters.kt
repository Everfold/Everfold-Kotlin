package com.everfold.core.liveactivity

import android.content.Context

internal fun formatShortChipText(
    context: Context,
    kind: LiveActivityEventKind,
): String =
    when (kind) {
        LiveActivityEventKind.AWAITING_INPUT -> context.getString(R.string.live_chip_attention)
        LiveActivityEventKind.FAILED -> context.getString(R.string.live_chip_failed)
        LiveActivityEventKind.COMPLETED -> context.getString(R.string.live_chip_attention)
    }

internal fun formatEventTitle(
    context: Context,
    event: LiveActivityEvent,
): String =
    when (event.kind) {
        LiveActivityEventKind.AWAITING_INPUT -> context.getString(R.string.live_alert_title_attention, event.sessionTitle)
        LiveActivityEventKind.FAILED -> context.getString(R.string.live_alert_title_failed, event.sessionTitle)
        LiveActivityEventKind.COMPLETED -> context.getString(R.string.live_alert_title_completed, event.sessionTitle)
    }

internal fun formatEventBody(
    context: Context,
    event: LiveActivityEvent,
): String =
    event.detailText.ifBlank {
        when (event.kind) {
            LiveActivityEventKind.AWAITING_INPUT -> context.getString(R.string.live_status_attention)
            LiveActivityEventKind.FAILED -> context.getString(R.string.live_status_failed)
            LiveActivityEventKind.COMPLETED -> context.getString(R.string.live_status_completed)
        }
    }

internal fun formatContextSubText(event: LiveActivityEvent): String {
    val host = event.hostNodeName.trim()
    val workspace = event.workspaceName.trim()
    return when {
        host.isNotEmpty() && workspace.isNotEmpty() -> "$host · $workspace"
        host.isNotEmpty() -> host
        else -> workspace
    }
}

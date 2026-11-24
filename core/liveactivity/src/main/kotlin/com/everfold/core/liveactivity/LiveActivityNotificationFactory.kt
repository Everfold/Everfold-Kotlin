package com.everfold.core.liveactivity

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

private const val PROGRESS_MAX = 100
private const val SEGMENT_PLAN_LEN = 20
private const val SEGMENT_EXEC_LEN = 65
private const val SEGMENT_DONE_LEN = 15
private const val COLOR_PLAN = 0xFF5C6BC0.toInt()
private const val COLOR_EXEC_ACTIVE = 0xFF26A69A.toInt()
private const val COLOR_EXEC_ERROR = 0xFFEF5350.toInt()
private const val COLOR_EXEC_ATTENTION = 0xFFFFA726.toInt()
private const val COLOR_EXEC_PAUSED = 0xFF78909C.toInt()
private const val COLOR_DONE = 0xFF42A5F5.toInt()
private const val TERMINAL_CAPSULE_TIMEOUT_MS = 300_000L

data class LiveActivityActionIntents(
    val openSession: PendingIntent,
    val pauseLoop: PendingIntent? = null,
    val resumeLoop: PendingIntent? = null,
    val stopLoop: PendingIntent? = null,
)

object LiveActivityNotificationFactory {
    fun buildLiveNotification(
        context: Context,
        snapshot: LiveActivitySnapshot,
        actions: LiveActivityActionIntents,
    ): Notification {
        val subText = formatContextSubText(snapshot)
        val builder =
            NotificationCompat
                .Builder(context, LiveActivityChannels.CHANNEL_LIVE_PROGRESS)
                .setSmallIcon(R.drawable.ic_stat_everfold)
                .setContentTitle(snapshot.sessionTitle)
                .setContentText(formatLiveBodyText(context, snapshot))
                .setOngoing(!snapshot.phase.isTerminal)
                .setAutoCancel(snapshot.phase.isTerminal)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(formatShortChipText(context, snapshot))
                .setContentIntent(actions.openSession)

        if (subText.isNotEmpty()) {
            builder.setSubText(subText)
        }
        if (snapshot.phase.isTerminal) {
            builder.setTimeoutAfter(TERMINAL_CAPSULE_TIMEOUT_MS)
        }

        applyProgressRepresentation(builder = builder, snapshot = snapshot)
        appendLiveActions(context = context, builder = builder, phase = snapshot.phase, actions = actions)
        return builder.build()
    }

    fun buildAlertNotification(
        context: Context,
        snapshot: LiveActivitySnapshot,
        openIntent: PendingIntent,
    ): Notification {
        val subText = formatContextSubText(snapshot)
        val body = formatLiveBodyText(context, snapshot)
        val builder =
            NotificationCompat
                .Builder(context, LiveActivityChannels.CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_everfold)
                .setContentTitle(formatAlertTitle(context, snapshot))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(
                    if (snapshot.phase == LiveActivityPhase.FAILED) {
                        NotificationCompat.CATEGORY_ERROR
                    } else {
                        NotificationCompat.CATEGORY_STATUS
                    },
                ).setAutoCancel(true)
                .setContentIntent(openIntent)

        if (subText.isNotEmpty()) {
            builder.setSubText(subText)
        }
        return builder.build()
    }

    private fun applyProgressRepresentation(
        builder: NotificationCompat.Builder,
        snapshot: LiveActivitySnapshot,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setStyle(buildPromotedProgressStyle(snapshot))
        } else {
            val indeterminate = snapshot.phase == LiveActivityPhase.PLANNING
            builder.setProgress(PROGRESS_MAX, computeProgressPercent(snapshot), indeterminate)
            builder.setStyle(
                NotificationCompat.BigTextStyle().bigText(snapshot.statusDetail.ifBlank { snapshot.sessionTitle }),
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildPromotedProgressStyle(snapshot: LiveActivitySnapshot): NotificationCompat.ProgressStyle {
        val execColor =
            when (snapshot.phase) {
                LiveActivityPhase.FAILED -> COLOR_EXEC_ERROR
                LiveActivityPhase.AWAITING_INPUT -> COLOR_EXEC_ATTENTION
                LiveActivityPhase.PAUSED -> COLOR_EXEC_PAUSED
                else -> COLOR_EXEC_ACTIVE
            }
        val style =
            NotificationCompat
                .ProgressStyle()
                .setProgressSegments(
                    listOf(
                        NotificationCompat.ProgressStyle.Segment(SEGMENT_PLAN_LEN).setColor(COLOR_PLAN),
                        NotificationCompat.ProgressStyle.Segment(SEGMENT_EXEC_LEN).setColor(execColor),
                        NotificationCompat.ProgressStyle.Segment(SEGMENT_DONE_LEN).setColor(COLOR_DONE),
                    ),
                )
        return if (snapshot.phase == LiveActivityPhase.PLANNING) {
            style.setProgressIndeterminate(true)
        } else {
            style.setProgress(computeProgressPercent(snapshot))
        }
    }

    private fun appendLiveActions(
        context: Context,
        builder: NotificationCompat.Builder,
        phase: LiveActivityPhase,
        actions: LiveActivityActionIntents,
    ) {
        when (phase) {
            LiveActivityPhase.PLANNING, LiveActivityPhase.EXECUTING -> {
                actions.pauseLoop?.let {
                    builder.addAction(0, context.getString(R.string.live_action_pause), it)
                }
                actions.stopLoop?.let {
                    builder.addAction(0, context.getString(R.string.live_action_stop), it)
                }
            }

            LiveActivityPhase.PAUSED -> {
                actions.resumeLoop?.let {
                    builder.addAction(0, context.getString(R.string.live_action_resume), it)
                }
                actions.stopLoop?.let {
                    builder.addAction(0, context.getString(R.string.live_action_stop), it)
                }
            }

            LiveActivityPhase.AWAITING_INPUT, LiveActivityPhase.COMPLETED, LiveActivityPhase.FAILED -> {
                builder.addAction(0, context.getString(R.string.live_action_open), actions.openSession)
            }
        }
    }
}

package com.everfold.kotlin

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.everfold.kotlin.mock.longtask.TaskNotificationKind
import com.everfold.kotlin.mock.longtask.TaskNotificationPayload
import com.everfold.kotlin.mock.longtask.TaskStageSegmentKind

internal const val EXTRA_CONVERSATION_ID = "com.everfold.kotlin.extra.CONVERSATION_ID"
internal const val ACTION_SYNC_TASK = "com.everfold.kotlin.action.SYNC_TASK"
internal const val ACTION_PAUSE_TASK = "com.everfold.kotlin.action.PAUSE_TASK"
internal const val ACTION_RESUME_TASK = "com.everfold.kotlin.action.RESUME_TASK"

internal const val ONGOING_NOTIFICATION_ID = 4101
internal const val RESULT_NOTIFICATION_ID = 4102
private const val RESULT_ID_MASK = 0x7FFF

private const val LEGACY_CHANNEL_ONGOING = "everfold_task_ongoing"
private const val CHANNEL_ONGOING = "everfold_task_live"
private const val CHANNEL_ATTENTION = "everfold_task_attention"
private const val MAX_NOTIFICATION_PROGRESS = 100
private const val OPEN_REQUEST_SALT = 1000
private const val ACTION_REQUEST_SALT = 2000

private const val COLOR_LLM = 0xFF6750A4.toInt()
private const val COLOR_TOOL = 0xFF00796B.toInt()
private const val COLOR_GATE = 0xFFE65100.toInt()
private const val COLOR_FAILED = 0xFFC62828.toInt()
private const val COLOR_PAUSED = 0xFF546E7A.toInt()
private const val COLOR_PENDING = 0xFFCFD8DC.toInt()

internal fun resultNotificationIdFor(conversationId: String): Int = RESULT_NOTIFICATION_ID + (conversationId.hashCode() and RESULT_ID_MASK)

internal object TaskNotificationFactory {
    fun ensureChannels(
        context: Context,
        manager: NotificationManager,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        manager.deleteNotificationChannel(LEGACY_CHANNEL_ONGOING)
        val ongoing =
            NotificationChannel(
                CHANNEL_ONGOING,
                context.getString(R.string.task_channel_ongoing),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        val attention =
            NotificationChannel(
                CHANNEL_ATTENTION,
                context.getString(R.string.task_channel_attention),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        manager.createNotificationChannels(listOf(ongoing, attention))
    }

    fun build(
        context: Context,
        payload: TaskNotificationPayload,
    ): Notification {
        val channelId = if (payload.kind == TaskNotificationKind.Running) CHANNEL_ONGOING else CHANNEL_ATTENTION
        val builder = createBuilder(context, channelId)
        val ongoing = payload.kind != TaskNotificationKind.Completed
        val clampedProgress = payload.progressPercent.coerceIn(0, MAX_NOTIFICATION_PROGRESS)
        builder
            .setSmallIcon(R.drawable.ic_stat_task)
            .setColor(resolveAccentColor(payload))
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setContentIntent(createOpenConversationIntent(context, payload.conversationId))
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setCategory(Notification.CATEGORY_PROGRESS)
        payload.concurrentSummary?.takeIf { it.isNotBlank() }?.let { summary ->
            builder.setSubText(summary)
        }
        applyProgressAndLiveUpdate(context, builder, payload, clampedProgress, ongoing)
        payload.actionLabel?.let { label ->
            val action = if (payload.kind == TaskNotificationKind.Running) ACTION_PAUSE_TASK else ACTION_RESUME_TASK
            val pending = createServiceActionIntent(context, action, payload.conversationId)
            builder.addAction(Notification.Action.Builder(null, label, pending).build())
        }
        return builder.build()
    }

    private fun applyProgressAndLiveUpdate(
        context: Context,
        builder: Notification.Builder,
        payload: TaskNotificationPayload,
        clampedProgress: Int,
        ongoing: Boolean,
    ) {
        if (payload.kind == TaskNotificationKind.Completed) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder
                .setStyle(buildMultiSegmentProgressStyle(context, payload, clampedProgress))
                .setRequestPromotedOngoing(ongoing)
            if (ongoing) {
                val chip =
                    payload.shortChipLabel?.takeIf { it.isNotBlank() }
                        ?: shortChipText(context, payload, clampedProgress)
                builder.setShortCriticalText(chip)
            }
        } else {
            builder.setProgress(MAX_NOTIFICATION_PROGRESS, clampedProgress, false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun buildMultiSegmentProgressStyle(
        context: Context,
        payload: TaskNotificationPayload,
        clampedProgress: Int,
    ): Notification.ProgressStyle {
        val style =
            Notification
                .ProgressStyle()
                .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.ic_stat_task))
                .setProgress(clampedProgress)
        if (payload.stageSegments.isEmpty()) {
            return style
                .addProgressSegment(Notification.ProgressStyle.Segment(MAX_NOTIFICATION_PROGRESS))
                .setStyledByProgress(true)
        }
        payload.stageSegments.forEach { segment ->
            val color = if (segment.reached) resolveStageColor(segment.kind) else COLOR_PENDING
            style.addProgressSegment(
                Notification.ProgressStyle
                    .Segment(segment.weight.coerceAtLeast(1))
                    .setColor(color),
            )
        }
        payload.gatePointPercent?.let { gatePercent ->
            style.addProgressPoint(
                Notification.ProgressStyle
                    .Point(gatePercent.coerceIn(1, MAX_NOTIFICATION_PROGRESS - 1))
                    .setColor(COLOR_GATE),
            )
        }
        return style.setStyledByProgress(false)
    }

    private fun resolveAccentColor(payload: TaskNotificationPayload): Int =
        when (payload.kind) {
            TaskNotificationKind.Waiting -> COLOR_GATE
            TaskNotificationKind.Failed -> COLOR_FAILED
            TaskNotificationKind.Paused -> COLOR_PAUSED
            TaskNotificationKind.Running, TaskNotificationKind.Completed -> resolveStageColor(payload.activeStageKind)
        }

    private fun resolveStageColor(kind: TaskStageSegmentKind): Int =
        when (kind) {
            TaskStageSegmentKind.Llm -> COLOR_LLM
            TaskStageSegmentKind.Tool -> COLOR_TOOL
            TaskStageSegmentKind.Gate -> COLOR_GATE
            TaskStageSegmentKind.Pending -> COLOR_PENDING
        }

    private fun shortChipText(
        context: Context,
        payload: TaskNotificationPayload,
        clampedProgress: Int,
    ): String =
        when (payload.kind) {
            TaskNotificationKind.Running -> "$clampedProgress%"
            TaskNotificationKind.Waiting -> context.getString(R.string.task_chip_waiting)
            TaskNotificationKind.Paused -> context.getString(R.string.task_chip_paused)
            TaskNotificationKind.Failed -> context.getString(R.string.task_chip_failed)
            TaskNotificationKind.Completed -> ""
        }

    private fun createBuilder(
        context: Context,
        channelId: String,
    ): Notification.Builder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

    private fun createOpenConversationIntent(
        context: Context,
        conversationId: String,
    ): PendingIntent {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
        val requestCode = OPEN_REQUEST_SALT + (conversationId.hashCode() and Int.MAX_VALUE) % OPEN_REQUEST_SALT
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createServiceActionIntent(
        context: Context,
        action: String,
        conversationId: String,
    ): PendingIntent {
        val intent =
            Intent(context, EverfoldTaskService::class.java).apply {
                this.action = action
                putExtra(EXTRA_CONVERSATION_ID, conversationId)
            }
        val requestCode = ACTION_REQUEST_SALT + ((action.hashCode() xor conversationId.hashCode()) and Int.MAX_VALUE) % ACTION_REQUEST_SALT
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

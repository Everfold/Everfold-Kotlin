package com.everfold.core.liveactivity

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

private const val ATTENTION_CAPSULE_TIMEOUT_MS = 600_000L

object LiveActivityNotificationFactory {
    fun buildEventNotification(
        context: Context,
        event: LiveActivityEvent,
        openIntent: PendingIntent,
    ): Notification {
        val subText = formatContextSubText(event)
        val body = formatEventBody(context, event)
        val builder =
            NotificationCompat
                .Builder(context, LiveActivityChannels.CHANNEL_ALERTS)
                .setSmallIcon(R.drawable.ic_stat_everfold)
                .setContentTitle(formatEventTitle(context, event))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(
                    if (event.kind == LiveActivityEventKind.FAILED) {
                        NotificationCompat.CATEGORY_ERROR
                    } else {
                        NotificationCompat.CATEGORY_STATUS
                    },
                ).setContentIntent(openIntent)

        if (subText.isNotEmpty()) {
            builder.setSubText(subText)
        }

        if (event.promoteAsCapsule) {
            builder
                .setOngoing(true)
                .setAutoCancel(false)
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(formatShortChipText(context, event.kind))
                .setTimeoutAfter(ATTENTION_CAPSULE_TIMEOUT_MS)
                .addAction(0, context.getString(R.string.live_action_open), openIntent)
        } else {
            builder
                .setOngoing(false)
                .setAutoCancel(true)
        }
        return builder.build()
    }
}

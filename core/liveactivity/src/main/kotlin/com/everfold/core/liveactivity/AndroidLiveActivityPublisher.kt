package com.everfold.core.liveactivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

private const val NOTIFICATION_ID_BASE = 42000
private const val SESSION_HASH_MASK = 0x7FFF

internal class AndroidLiveActivityPublisher(
    private val context: Context,
    private val launchIntentFactory: LiveActivityLaunchIntentFactory,
) : LiveActivityPublisher {
    private val notificationManager = NotificationManagerCompat.from(context)

    override fun publishEvent(event: LiveActivityEvent) {
        if (!canPostNotifications()) return
        LiveActivityChannels.ensureChannels(context)
        val id = notificationId(event.sessionId)
        val openPendingIntent = buildOpenSessionPendingIntent(event.sessionId, id)
        val notification = LiveActivityNotificationFactory.buildEventNotification(context, event, openPendingIntent)
        notifySafely(id, notification)
    }

    override fun dismissSession(sessionId: String) {
        notificationManager.cancel(notificationId(sessionId))
    }

    private fun canPostNotifications(): Boolean {
        val granted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        return granted && notificationManager.areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    private fun notifySafely(
        notificationId: Int,
        notification: Notification,
    ) {
        runCatching {
            notificationManager.notify(notificationId, notification)
        }.onFailure { error ->
            Timber.w(error, "Failed to post notification %d", notificationId)
        }
    }

    private fun buildOpenSessionPendingIntent(
        sessionId: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = launchIntentFactory.createOpenSessionIntent(sessionId)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(sessionId: String): Int = NOTIFICATION_ID_BASE + (sessionId.hashCode() and SESSION_HASH_MASK)
}

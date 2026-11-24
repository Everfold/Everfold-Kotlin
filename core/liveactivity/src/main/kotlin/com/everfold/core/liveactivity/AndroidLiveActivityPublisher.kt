package com.everfold.core.liveactivity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

private const val ALERT_ID_BASE = 42000
private const val SESSION_HASH_MASK = 0x7FFF
private const val REQ_CODE_OPEN_CAPSULE = 4100
private const val REQ_CODE_PAUSE = 4102
private const val REQ_CODE_RESUME = 4103
private const val REQ_CODE_STOP = 4104

internal class AndroidLiveActivityPublisher(
    private val context: Context,
    private val launchIntentFactory: LiveActivityLaunchIntentFactory,
) : LiveActivityPublisher {
    private val notificationManager = NotificationManagerCompat.from(context)

    @Volatile
    private var isServiceActive = false

    @Volatile
    private var hasPostedCapsule = false

    override fun publishCapsule(snapshot: LiveActivitySnapshot) {
        if (!canPostNotifications()) return
        LiveActivityChannels.ensureChannels(context)

        val openPendingIntent = buildOpenSessionPendingIntent(snapshot.sessionId, REQ_CODE_OPEN_CAPSULE)
        val actions =
            LiveActivityActionIntents(
                openSession = openPendingIntent,
                pauseLoop = buildLoopControlPendingIntent(LoopControlBroadcastReceiver.ACTION_PAUSE, snapshot.sessionId, REQ_CODE_PAUSE),
                resumeLoop =
                    buildLoopControlPendingIntent(
                        LoopControlBroadcastReceiver.ACTION_RESUME,
                        snapshot.sessionId,
                        REQ_CODE_RESUME,
                    ),
                stopLoop = buildLoopControlPendingIntent(LoopControlBroadcastReceiver.ACTION_STOP, snapshot.sessionId, REQ_CODE_STOP),
            )
        val notification = LiveActivityNotificationFactory.buildLiveNotification(context, snapshot, actions)
        LiveActivityNotificationHolder.currentNotification = notification
        hasPostedCapsule = true

        if (snapshot.phase.isTerminal) {
            if (isServiceActive) {
                isServiceActive = false
                dispatchServiceAction(AgentLiveActivityService.ACTION_DETACH_TERMINAL, foreground = false)
            }
            notifySafely(AgentLiveActivityService.LIVE_NOTIFICATION_ID, notification)
        } else {
            val started = dispatchServiceAction(AgentLiveActivityService.ACTION_START_OR_UPDATE, foreground = true)
            isServiceActive = started
            if (!started) {
                notifySafely(AgentLiveActivityService.LIVE_NOTIFICATION_ID, notification)
            }
        }
    }

    override fun dismissCapsule() {
        LiveActivityNotificationHolder.currentNotification = null
        if (isServiceActive) {
            isServiceActive = false
            dispatchServiceAction(AgentLiveActivityService.ACTION_STOP_AND_REMOVE, foreground = false)
        }
        if (hasPostedCapsule) {
            hasPostedCapsule = false
            notificationManager.cancel(AgentLiveActivityService.LIVE_NOTIFICATION_ID)
        }
    }

    override fun publishAlert(snapshot: LiveActivitySnapshot) {
        if (!canPostNotifications()) return
        LiveActivityChannels.ensureChannels(context)
        val alertId = alertNotificationId(snapshot.sessionId)
        val openPendingIntent = buildOpenSessionPendingIntent(snapshot.sessionId, alertId)
        val notification = LiveActivityNotificationFactory.buildAlertNotification(context, snapshot, openPendingIntent)
        notifySafely(alertId, notification)
    }

    override fun dismissAlert(sessionId: String) {
        notificationManager.cancel(alertNotificationId(sessionId))
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

    private fun dispatchServiceAction(
        action: String,
        foreground: Boolean,
    ): Boolean {
        val serviceIntent = Intent(context, AgentLiveActivityService::class.java).setAction(action)
        return runCatching {
            if (foreground) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            true
        }.onFailure { error ->
            Timber.w(error, "Foreground service transition skipped for action %s", action)
        }.getOrDefault(false)
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

    private fun buildLoopControlPendingIntent(
        action: String,
        sessionId: String,
        requestCode: Int,
    ): PendingIntent {
        val intent =
            Intent(context, LoopControlBroadcastReceiver::class.java)
                .setAction(action)
                .putExtra(LoopControlBroadcastReceiver.EXTRA_SESSION_ID, sessionId)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alertNotificationId(sessionId: String): Int = ALERT_ID_BASE + (sessionId.hashCode() and SESSION_HASH_MASK)
}

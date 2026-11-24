package com.everfold.core.liveactivity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object LiveActivityChannels {
    const val CHANNEL_LIVE_PROGRESS = "everfold_loop_live"
    const val CHANNEL_ALERTS = "everfold_loop_alerts"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val progressChannel =
            NotificationChannel(
                CHANNEL_LIVE_PROGRESS,
                context.getString(R.string.live_channel_progress_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.live_channel_progress_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            }
        val alertChannel =
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.live_channel_alert_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.live_channel_alert_desc)
                enableVibration(true)
            }
        manager.createNotificationChannels(listOf(progressChannel, alertChannel))
    }
}

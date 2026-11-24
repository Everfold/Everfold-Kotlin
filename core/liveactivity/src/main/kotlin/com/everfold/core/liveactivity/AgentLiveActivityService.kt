package com.everfold.core.liveactivity

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat

internal object LiveActivityNotificationHolder {
    @Volatile
    var currentNotification: Notification? = null
}

class AgentLiveActivityService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_DETACH_TERMINAL -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                stopSelf()
            }

            ACTION_STOP_AND_REMOVE -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }

            else -> {
                val notification = LiveActivityNotificationHolder.currentNotification
                if (notification == null) {
                    stopSelf()
                } else {
                    val serviceType =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                        } else {
                            0
                        }
                    ServiceCompat.startForeground(this, LIVE_NOTIFICATION_ID, notification, serviceType)
                }
            }
        }
        return START_NOT_STICKY
    }

    internal companion object {
        const val LIVE_NOTIFICATION_ID = 4101
        const val ACTION_START_OR_UPDATE = "com.everfold.liveactivity.ACTION_START_OR_UPDATE"
        const val ACTION_DETACH_TERMINAL = "com.everfold.liveactivity.ACTION_DETACH_TERMINAL"
        const val ACTION_STOP_AND_REMOVE = "com.everfold.liveactivity.ACTION_STOP_AND_REMOVE"
    }
}

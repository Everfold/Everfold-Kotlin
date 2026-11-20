package com.everfold.kotlin

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.everfold.kotlin.mock.longtask.AndroidLongTaskRuntime
import com.everfold.kotlin.mock.longtask.TaskNotificationKind
import com.everfold.kotlin.mock.longtask.TaskNotificationPayload

/** 承载本机单智能体长程任务的前台服务，维护进度通知与暂停/继续操作。 */
class EverfoldTaskService : Service() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        TaskNotificationFactory.ensureChannels(this, manager)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        val conversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID)
        when (intent?.action) {
            ACTION_PAUSE_TASK -> if (conversationId != null) AndroidLongTaskRuntime.dispatchPause(conversationId)
            ACTION_RESUME_TASK -> if (conversationId != null) AndroidLongTaskRuntime.dispatchResume(conversationId)
            else -> applyPayload(AndroidLongTaskRuntime.currentNotification.value)
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        AndroidLongTaskRuntime.dispatchTimeoutStop()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun applyPayload(payload: TaskNotificationPayload?) {
        val manager = getSystemService(NotificationManager::class.java)
        if (payload == null) {
            val placeholder =
                TaskNotificationPayload(
                    conversationId = "",
                    title = getString(R.string.app_name),
                    body = "",
                    progressPercent = 0,
                    kind = TaskNotificationKind.Completed,
                    actionLabel = null,
                )
            promoteToForeground(TaskNotificationFactory.build(this, placeholder))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        val notification = TaskNotificationFactory.build(this, payload)
        promoteToForeground(notification)
        if (payload.kind == TaskNotificationKind.Completed) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            manager.notify(RESULT_NOTIFICATION_ID, notification)
            stopSelf()
        } else if (payload.kind != TaskNotificationKind.Running) {
            stopForeground(STOP_FOREGROUND_DETACH)
            manager.notify(ONGOING_NOTIFICATION_ID, notification)
            stopSelf()
        }
    }

    private fun promoteToForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ONGOING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        fun sync(
            context: Context,
            payload: TaskNotificationPayload?,
        ) {
            val manager = context.getSystemService(NotificationManager::class.java)
            TaskNotificationFactory.ensureChannels(context, manager)
            if (payload == null) {
                manager.cancel(ONGOING_NOTIFICATION_ID)
                context.stopService(Intent(context, EverfoldTaskService::class.java))
                return
            }
            val serviceIntent =
                Intent(context, EverfoldTaskService::class.java).apply {
                    action = ACTION_SYNC_TASK
                    putExtra(EXTRA_CONVERSATION_ID, payload.conversationId)
                }
            if (payload.kind == TaskNotificationKind.Running) {
                startServiceSafely(context, manager, payload, serviceIntent)
            } else {
                // 非 Running 态若服务已在运行则通过服务降级前台状态，否则直接刷新普通通知避免后台拉起 FGS 异常
                context.stopService(Intent(context, EverfoldTaskService::class.java))
                val notificationId =
                    if (payload.kind == TaskNotificationKind.Completed) {
                        manager.cancel(ONGOING_NOTIFICATION_ID)
                        RESULT_NOTIFICATION_ID
                    } else {
                        ONGOING_NOTIFICATION_ID
                    }
                manager.notify(notificationId, TaskNotificationFactory.build(context, payload))
            }
        }

        private fun startServiceSafely(
            context: Context,
            manager: NotificationManager,
            payload: TaskNotificationPayload,
            serviceIntent: Intent,
        ) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } catch (_: IllegalStateException) {
                manager.notify(ONGOING_NOTIFICATION_ID, TaskNotificationFactory.build(context, payload))
            }
        }
    }
}

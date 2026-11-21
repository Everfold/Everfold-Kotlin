package com.everfold.kotlin

import android.app.Notification
import android.app.NotificationManager
import android.app.RemoteInput
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.everfold.kotlin.mock.longtask.AndroidLongTaskRuntime
import com.everfold.kotlin.mock.longtask.TaskNotificationKind
import com.everfold.kotlin.mock.longtask.TaskNotificationPayload

private const val WAKE_LOCK_TAG = "Everfold:LongTaskWakeLock"
private const val WAKE_LOCK_TIMEOUT_MS = 5 * 60 * 1_000L

/** 承载本机单智能体长程任务的前台服务，维护后台协程唤醒锁、进度通知与暂停/继续操作。 */
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
        if (conversationId != null) {
            when (intent.action) {
                ACTION_PAUSE_TASK -> {
                    AndroidLongTaskRuntime.dispatchPause(conversationId)
                }

                ACTION_RESUME_TASK -> {
                    AndroidLongTaskRuntime.dispatchResume(conversationId)
                }

                ACTION_RESOLVE_GATE_OPTION -> {
                    val optionId = intent.getStringExtra(EXTRA_GATE_OPTION_ID)
                    if (optionId != null) AndroidLongTaskRuntime.dispatchResolveGateOption(conversationId, optionId)
                }

                ACTION_REPLY_GATE_CONSTRAINT -> {
                    val replyText =
                        RemoteInput
                            .getResultsFromIntent(intent)
                            ?.getCharSequence(KEY_GATE_CONSTRAINT_REPLY)
                            ?.toString()
                            ?.trim()
                    if (!replyText.isNullOrEmpty()) AndroidLongTaskRuntime.dispatchGateConstraintReply(conversationId, replyText)
                }
            }
        }
        applyPayload(AndroidLongTaskRuntime.currentNotification.value)
        return START_NOT_STICKY
    }

    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        isServiceForeground = false
        syncWakeLock(this, shouldHold = false)
        AndroidLongTaskRuntime.dispatchTimeoutStop()
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf(startId)
    }

    override fun onDestroy() {
        isServiceForeground = false
        syncWakeLock(this, shouldHold = false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun applyPayload(payload: TaskNotificationPayload?) {
        val manager = getSystemService(NotificationManager::class.java)
        if (payload == null) {
            isServiceForeground = false
            syncWakeLock(this, shouldHold = false)
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
        isServiceForeground = true
        syncWakeLock(this, shouldHold = payload.kind == TaskNotificationKind.Running)
        if (payload.kind == TaskNotificationKind.Completed) {
            isServiceForeground = false
            syncWakeLock(this, shouldHold = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            manager.notify(resultNotificationIdFor(payload.conversationId), notification)
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
        @Volatile
        private var isServiceForeground: Boolean = false

        @Volatile
        private var activeWakeLock: PowerManager.WakeLock? = null

        fun sync(
            context: Context,
            payload: TaskNotificationPayload?,
        ) {
            val manager = context.getSystemService(NotificationManager::class.java)
            TaskNotificationFactory.ensureChannels(context, manager)
            when {
                payload == null -> {
                    isServiceForeground = false
                    syncWakeLock(context, shouldHold = false)
                    manager.cancel(ONGOING_NOTIFICATION_ID)
                    context.stopService(Intent(context, EverfoldTaskService::class.java))
                }

                payload.kind == TaskNotificationKind.Completed -> {
                    manager.notify(
                        resultNotificationIdFor(payload.conversationId),
                        TaskNotificationFactory.build(context, payload),
                    )
                }

                isServiceForeground -> {
                    // 服务已处于前台时直接通过 NotificationManager 原地刷新进度，避免后台高频调用 startForegroundService 被系统拦截
                    syncWakeLock(context, shouldHold = payload.kind == TaskNotificationKind.Running)
                    manager.notify(ONGOING_NOTIFICATION_ID, TaskNotificationFactory.build(context, payload))
                }

                else -> {
                    syncWakeLock(context, shouldHold = payload.kind == TaskNotificationKind.Running)
                    val serviceIntent =
                        Intent(context, EverfoldTaskService::class.java).apply {
                            action = ACTION_SYNC_TASK
                            putExtra(EXTRA_CONVERSATION_ID, payload.conversationId)
                        }
                    startServiceSafely(context, manager, payload, serviceIntent)
                }
            }
        }

        private fun syncWakeLock(
            context: Context,
            shouldHold: Boolean,
        ) {
            if (shouldHold) {
                val lock =
                    activeWakeLock
                        ?: context.applicationContext
                            .getSystemService(PowerManager::class.java)
                            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
                            ?.apply { setReferenceCounted(false) }
                            ?.also { activeWakeLock = it }
                if (lock != null && !lock.isHeld) {
                    lock.acquire(WAKE_LOCK_TIMEOUT_MS)
                }
            } else {
                activeWakeLock?.takeIf { it.isHeld }?.release()
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

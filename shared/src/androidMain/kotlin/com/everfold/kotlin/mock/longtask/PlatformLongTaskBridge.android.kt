package com.everfold.kotlin.mock.longtask

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.everfold.kotlin.mock.MockWorkspace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val NOTIFICATION_THROTTLE_MS = 750L

/** Android 宿主桥接入口，供 androidApp 的前台服务与 MainActivity 交互。 */
object AndroidLongTaskRuntime {
    private val commandFlow = MutableSharedFlow<DeviceTaskCommand>(extraBufferCapacity = 16)
    private val openConversationState = MutableStateFlow<String?>(null)
    private val notificationState = MutableStateFlow<TaskNotificationPayload?>(null)
    private val aggregator = LongTaskNotificationAggregator()
    private val lastPublishElapsedByConv = mutableMapOf<String, Long>()
    private val lastPublishedKeyByConv = mutableMapOf<String, String>()
    private var appContext: Context? = null
    private var hostSync: ((Context, TaskNotificationPayload?) -> Unit)? = null

    val currentNotification: StateFlow<TaskNotificationPayload?> = notificationState.asStateFlow()

    internal val bridge: PlatformLongTaskBridge =
        object : PlatformLongTaskBridge {
            override val commands: SharedFlow<DeviceTaskCommand> = commandFlow.asSharedFlow()
            override val pendingOpenConversationId: StateFlow<String?> = openConversationState.asStateFlow()

            override fun publish(payload: TaskNotificationPayload) {
                val now = SystemClock.elapsedRealtime()
                val convId = payload.conversationId
                val stateKey = "$convId:${payload.kind}:${payload.activeStageKind}:${payload.title}"
                val lastElapsed = lastPublishElapsedByConv[convId] ?: 0L
                val shouldSkip =
                    payload.kind == TaskNotificationKind.Running &&
                        stateKey == lastPublishedKeyByConv[convId] &&
                        now - lastElapsed < NOTIFICATION_THROTTLE_MS
                if (shouldSkip) return

                lastPublishElapsedByConv[convId] = now
                lastPublishedKeyByConv[convId] = stateKey
                val update = aggregator.onPublish(payload, isEnglishNotificationLocale())
                notificationState.value = update.primaryOngoing
                update.completedEvent?.let { completed ->
                    lastPublishElapsedByConv.remove(completed.conversationId)
                    lastPublishedKeyByConv.remove(completed.conversationId)
                    notifyHost(completed)
                }
                notifyHost(update.primaryOngoing)
            }

            override fun dismiss(conversationId: String) {
                lastPublishElapsedByConv.remove(conversationId)
                lastPublishedKeyByConv.remove(conversationId)
                val update = aggregator.onDismiss(conversationId, isEnglishNotificationLocale())
                if (notificationState.value != update.primaryOngoing) {
                    notificationState.value = update.primaryOngoing
                    notifyHost(update.primaryOngoing)
                }
            }

            override fun consumeOpenConversation(conversationId: String) {
                if (openConversationState.value == conversationId) {
                    openConversationState.value = null
                }
            }
        }

    fun bindHost(
        context: Context,
        onSync: (Context, TaskNotificationPayload?) -> Unit,
    ) {
        appContext = context.applicationContext
        hostSync = onSync
        notificationState.value?.let { onSync(context.applicationContext, it) }
    }

    fun dispatchPause(conversationId: String) {
        commandFlow.tryEmit(DeviceTaskCommand.Pause(conversationId))
    }

    fun dispatchResume(conversationId: String) {
        commandFlow.tryEmit(DeviceTaskCommand.Resume(conversationId))
    }

    fun dispatchResolveGateOption(
        conversationId: String,
        optionId: String,
    ) {
        commandFlow.tryEmit(DeviceTaskCommand.ResolveGateOption(conversationId, optionId))
    }

    fun dispatchGateConstraintReply(
        conversationId: String,
        constraintText: String,
    ) {
        commandFlow.tryEmit(DeviceTaskCommand.ReplyGateConstraint(conversationId, constraintText))
    }

    fun dispatchTimeoutStop() {
        commandFlow.tryEmit(DeviceTaskCommand.TimeoutStopAll)
    }

    fun requestOpenConversation(conversationId: String) {
        openConversationState.value = conversationId
    }

    private fun notifyHost(payload: TaskNotificationPayload?) {
        val ctx = appContext ?: return
        hostSync?.invoke(ctx, payload)
    }
}

private val ProcessWorkspaceInstance by lazy {
    MockWorkspace(
        enableLongTasks = true,
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        taskBridge = AndroidLongTaskRuntime.bridge,
    )
}

internal actual val platformLongTaskBridge: PlatformLongTaskBridge
    get() = AndroidLongTaskRuntime.bridge

internal actual fun isEnglishNotificationLocale(): Boolean = Locale.getDefault().language.equals("en", ignoreCase = true)

@Composable
internal actual fun rememberAppWorkspace(): MockWorkspace = ProcessWorkspaceInstance

@Composable
internal actual fun PlatformLongTaskPermissionEffect(hasActiveLocalTask: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(hasActiveLocalTask) {
        if (hasActiveLocalTask && !requested) {
            requested = true
            val granted =
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            if (!granted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

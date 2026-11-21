package com.everfold.kotlin.mock.longtask

import androidx.compose.runtime.Composable
import com.everfold.kotlin.mock.MockWorkspace
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class TaskNotificationKind {
    Running,
    Waiting,
    Paused,
    Failed,
    Completed,
}

enum class TaskStageSegmentKind {
    Llm,
    Tool,
    Gate,
    Pending,
}

data class TaskStageSegment(
    val weight: Int,
    val kind: TaskStageSegmentKind,
    val reached: Boolean,
)

data class TaskNotificationPayload(
    val conversationId: String,
    val title: String,
    val body: String,
    val progressPercent: Int,
    val kind: TaskNotificationKind,
    val actionLabel: String?,
    val stageSegments: List<TaskStageSegment> = emptyList(),
    val gatePointPercent: Int? = null,
    val activeStageKind: TaskStageSegmentKind = TaskStageSegmentKind.Llm,
    val concurrentActiveCount: Int = 1,
    val concurrentSummary: String? = null,
    val shortChipLabel: String? = null,
)

sealed interface DeviceTaskCommand {
    data class Pause(
        val conversationId: String,
    ) : DeviceTaskCommand

    data class Resume(
        val conversationId: String,
    ) : DeviceTaskCommand

    data object TimeoutStopAll : DeviceTaskCommand
}

internal interface PlatformLongTaskBridge {
    val commands: SharedFlow<DeviceTaskCommand>
    val pendingOpenConversationId: StateFlow<String?>

    fun publish(payload: TaskNotificationPayload)

    fun dismiss(conversationId: String)

    fun consumeOpenConversation(conversationId: String)
}

internal object NoopLongTaskBridge : PlatformLongTaskBridge {
    override val commands: SharedFlow<DeviceTaskCommand> = MutableSharedFlow()
    override val pendingOpenConversationId: StateFlow<String?> = MutableStateFlow(null)

    override fun publish(payload: TaskNotificationPayload) = Unit

    override fun dismiss(conversationId: String) = Unit

    override fun consumeOpenConversation(conversationId: String) = Unit
}

internal expect val platformLongTaskBridge: PlatformLongTaskBridge

internal expect fun isEnglishNotificationLocale(): Boolean

@Composable
internal expect fun rememberAppWorkspace(): MockWorkspace

@Composable
internal expect fun PlatformLongTaskPermissionEffect(hasActiveLocalTask: Boolean)

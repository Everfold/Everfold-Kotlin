package com.everfold.kotlin.feature.conversation

import androidx.compose.runtime.Immutable
import com.everfold.kotlin.ui.localization.UiText

enum class StepStatus {
    Pending,
    Running,
    WaitingApproval,
    Completed,
    Failed,
}

enum class DeviceNoteSeverity {
    Info,
    Warning,
}

@Immutable
data class GateOption(
    val id: String,
    val label: UiText,
)

@Immutable
data class WaitingGate(
    val id: String,
    val prompt: UiText,
    val riskHint: UiText? = null,
    val options: List<GateOption> = emptyList(),
)

@Immutable
data class DeviceEnvironmentNote(
    val severity: DeviceNoteSeverity,
    val message: UiText,
)

@Immutable
data class ExecutionStepSnapshot(
    val id: String,
    val title: UiText,
    val status: StepStatus,
    val toolName: String? = null,
    val commandPreview: UiText? = null,
    val observation: UiText? = null,
    val stepProgress: Float? = null,
    val durationMillis: Long? = null,
    val resumedFromCheckpoint: Boolean = false,
)

@Immutable
data class TaskExecutionTrace(
    val taskId: String,
    val headline: UiText,
    val currentStepIndex: Int,
    val totalSteps: Int,
    val overallProgress: Float,
    val elapsedMillis: Long,
    val steps: List<ExecutionStepSnapshot>,
    val deviceNote: DeviceEnvironmentNote? = null,
    val activeGate: WaitingGate? = null,
)

enum class DeviceTaskFault {
    PowerSaverThrottle,
    StorageIoInterrupt,
}

private object NoopDeviceTaskController : DeviceTaskController

/** 单智能体长程任务的附加控制入口（门控选项确认、环境故障注入），避免膨胀 Workspace 顶层方法数。 */
internal interface DeviceTaskController {
    fun resolveGateOption(
        conversationId: String,
        optionId: String,
    ) = Unit

    fun injectFault(
        conversationId: String,
        fault: DeviceTaskFault,
    ) = Unit

    companion object {
        val Noop: DeviceTaskController = NoopDeviceTaskController
    }
}

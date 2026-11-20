package com.everfold.kotlin.feature.conversation

import androidx.compose.runtime.Immutable
import com.everfold.kotlin.ui.localization.UiText

enum class StreamRunStatus {
    Running,
    Paused,
    Failed,
    Completed,
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

/** 单智能体实时交错信息流中的原子节点，按执行时序动态追加，不预先生成未到达节点。 */
@Immutable
sealed interface AgentStreamBlock {
    val id: String

    @Immutable
    data class Thinking(
        override val id: String,
        val text: String,
        val status: StreamRunStatus,
        val durationMillis: Long = 0L,
    ) : AgentStreamBlock

    @Immutable
    data class Narration(
        override val id: String,
        val text: String,
        val isStreaming: Boolean = false,
    ) : AgentStreamBlock

    @Immutable
    data class ToolCall(
        override val id: String,
        val toolName: String,
        val command: String,
        val status: StreamRunStatus,
        val summary: UiText,
        val logLines: List<String> = emptyList(),
        val progress: Float? = null,
        val durationMillis: Long = 0L,
        val resumed: Boolean = false,
    ) : AgentStreamBlock

    @Immutable
    data class Approval(
        override val id: String,
        val gate: WaitingGate,
        val resolvedChoice: UiText? = null,
    ) : AgentStreamBlock

    @Immutable
    data class SystemNotice(
        override val id: String,
        val note: DeviceEnvironmentNote,
    ) : AgentStreamBlock
}

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

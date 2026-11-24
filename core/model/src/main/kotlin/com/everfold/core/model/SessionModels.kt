package com.everfold.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.serialization.Serializable

@Serializable
enum class LoopState {
    IDLE,
    PLANNING,
    EXECUTING_ACTION,
    PAUSED,
    AWAITING_INPUT,
    COMPLETED,
    FAILED,
}

@Serializable
data class SessionAttachment(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val uriString: String = "",
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/", ignoreCase = true)
}

data class SessionRuntimeContext(
    val relayProfileId: String? = null,
    val relayName: String = "",
    val nodeId: String = "",
    val nodeName: String = "",
    val nodePlatform: String = "",
    val workspaceId: String? = null,
    val workspaceName: String = "",
    val workspaceFileScope: String = "",
    val workspacePermission: WorkspacePermissionLevel? = null,
    val workspaceCapabilities: ImmutableSet<WorkspaceCapability> = persistentSetOf(),
)

sealed interface LoopControlCommand {
    data object Pause : LoopControlCommand

    data class Resume(
        val steeringMessage: String? = null,
        val attachments: ImmutableList<SessionAttachment> = persistentListOf(),
    ) : LoopControlCommand

    data object Stop : LoopControlCommand
}

@Serializable
enum class ActionKind {
    LLM_REQUEST,
    TOOL_CALL,
    MCP_CALL,
    CODE_MODE,
}

@Serializable
enum class ActionStatus {
    RUNNING,
    SUCCEEDED,
    FAILED,
}

data class ActionTrace(
    val id: String,
    val kind: ActionKind,
    val title: String,
    val summary: String,
    val status: ActionStatus,
    val durationMs: Long? = null,
    val executedOn: NodeAttribution? = null,
)

sealed interface ObservedItem {
    val id: String

    data class UserMessage(
        override val id: String,
        val content: String,
        val timestampLabel: String,
        val attachments: ImmutableList<SessionAttachment> = persistentListOf(),
    ) : ObservedItem

    data class ActionGroup(
        override val id: String,
        val actions: ImmutableList<ActionTrace>,
    ) : ObservedItem

    data class AssistantMessage(
        override val id: String,
        val markdown: String,
        val isStreaming: Boolean = false,
        val timestampLabel: String,
    ) : ObservedItem
}

data class AgentSessionSummary(
    val id: String,
    val title: String,
    val modelId: String,
    val loopState: LoopState,
    val loopHostNodeName: String,
    val lastMessagePreview: String,
    val updatedAtLabel: String,
    val workspaceName: String = "Default",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val reasoningEffort: ReasoningEffort? = null,
    val runtimeContext: SessionRuntimeContext? = null,
)

data class AgentSessionDetail(
    val summary: AgentSessionSummary,
    val items: ImmutableList<ObservedItem>,
)

data class SessionSearchFilter(
    val workspaceName: String? = null,
    val nodeName: String? = null,
    val loopState: LoopState? = null,
    val includeArchived: Boolean = false,
) {
    val hasActiveFilters: Boolean
        get() = workspaceName != null || nodeName != null || loopState != null || includeArchived
}

@Serializable
enum class SessionMatchSource {
    TITLE,
    MESSAGE,
}

data class SessionSearchHit(
    val summary: AgentSessionSummary,
    val matchedSnippet: String? = null,
    val matchedItemId: String? = null,
    val matchSource: SessionMatchSource? = null,
)

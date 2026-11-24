package com.everfold.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable

@Serializable
enum class LoopState {
    IDLE,
    PLANNING,
    EXECUTING_ACTION,
    AWAITING_INPUT,
    COMPLETED,
    FAILED,
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

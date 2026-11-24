package com.everfold.feature.sessions

import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.ModelDescriptor
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.NodeStatus
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionAttachment
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.WorkspaceDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

data class SessionDetailUiState(
    val sessionId: String? = null,
    val detail: AgentSessionDetail? = null,
    val draft: String = "",
    val draftAttachments: ImmutableList<SessionAttachment> = persistentListOf(),
    val availableNodes: ImmutableList<NodeDescriptor> = persistentListOf(),
    val availableModels: ImmutableList<ModelDescriptor> = persistentListOf(),
    val hasConnectedRelay: Boolean = false,
    val activeContext: SessionRuntimeContext? = null,
    val activeModel: ModelDescriptor? = null,
    val activeEffort: ReasoningEffort? = null,
    val chatTextSize: ChatTextSize = ChatTextSize.MEDIUM,
    val autoCollapseLongMessages: Boolean = true,
    val expandRunningActionTraces: Boolean = true,
    val expandCompletedActionTraces: Boolean = false,
)

internal data class DraftInputState(
    val text: String = "",
    val attachments: ImmutableList<SessionAttachment> = persistentListOf(),
)

internal data class EnvironmentSelectionState(
    val nodes: ImmutableList<NodeDescriptor> = persistentListOf(),
    val models: ImmutableList<ModelDescriptor> = persistentListOf(),
    val hasConnectedRelay: Boolean = false,
    val selectedContext: SessionRuntimeContext? = null,
    val selectedModelId: String? = null,
    val selectedEffort: ReasoningEffort? = null,
)

internal fun buildSessionDetailUiState(
    sessionId: String?,
    detail: AgentSessionDetail?,
    draftState: DraftInputState,
    env: EnvironmentSelectionState,
    prefs: AppPreferences,
): SessionDetailUiState {
    val activeModel = resolveActiveModel(detail = detail, models = env.models, selectedModelId = env.selectedModelId)
    val activeEffort =
        resolveReasoningEffort(
            detail = detail,
            model = activeModel,
            selectedEffort = env.selectedEffort,
        )
    val activeContext =
        resolveRuntimeContext(
            sessionId = sessionId,
            detail = detail,
            nodes = env.nodes,
            selectedContext = env.selectedContext,
        )
    return SessionDetailUiState(
        sessionId = sessionId,
        detail = detail,
        draft = draftState.text,
        draftAttachments = draftState.attachments,
        availableNodes = env.nodes,
        availableModels = env.models.toImmutableList(),
        hasConnectedRelay = env.hasConnectedRelay,
        activeContext = activeContext,
        activeModel = activeModel,
        activeEffort = activeEffort,
        chatTextSize = prefs.chatTextSize,
        autoCollapseLongMessages = prefs.autoCollapseLongMessages,
        expandRunningActionTraces = prefs.expandRunningActionTraces,
        expandCompletedActionTraces = prefs.expandCompletedActionTraces,
    )
}

internal fun resolveActiveModel(
    detail: AgentSessionDetail?,
    models: List<ModelDescriptor>,
    selectedModelId: String?,
): ModelDescriptor? {
    val targetId = detail?.summary?.modelId ?: selectedModelId
    return models.firstOrNull { it.id == targetId }
        ?: models.firstOrNull { it.isDefault }
        ?: models.firstOrNull()
}

internal fun resolveReasoningEffort(
    detail: AgentSessionDetail?,
    model: ModelDescriptor?,
    selectedEffort: ReasoningEffort?,
): ReasoningEffort? {
    val supported = model?.supportedEfforts.orEmpty()
    val preferred = detail?.summary?.reasoningEffort ?: selectedEffort
    return when {
        supported.isEmpty() -> null
        preferred != null && preferred in supported -> preferred
        else -> model?.defaultEffort ?: supported.firstOrNull()
    }
}

internal fun resolveRuntimeContext(
    sessionId: String?,
    detail: AgentSessionDetail?,
    nodes: List<NodeDescriptor>,
    selectedContext: SessionRuntimeContext?,
): SessionRuntimeContext? =
    if (sessionId != null && detail != null) {
        detail.summary.runtimeContext ?: SessionRuntimeContext(
            nodeName = detail.summary.loopHostNodeName,
            workspaceName = detail.summary.workspaceName,
        )
    } else if (nodes.isEmpty()) {
        null
    } else {
        val validSelected = selectedContext?.takeIf { ctx -> nodes.any { it.id == ctx.nodeId } }
        validSelected ?: defaultRuntimeContext(nodes)
    }

private fun defaultRuntimeContext(nodes: List<NodeDescriptor>): SessionRuntimeContext? {
    val node = nodes.firstOrNull { it.status == NodeStatus.ONLINE } ?: nodes.firstOrNull() ?: return null
    val workspace = node.workspaces.firstOrNull()
    return toRuntimeContext(node = node, workspace = workspace)
}

internal fun toRuntimeContext(
    node: NodeDescriptor,
    workspace: WorkspaceDescriptor?,
): SessionRuntimeContext =
    SessionRuntimeContext(
        relayProfileId = node.relayProfileId,
        relayName = node.relayName,
        nodeId = node.id,
        nodeName = node.name,
        nodePlatform = node.platform,
        workspaceId = workspace?.id,
        workspaceName = workspace?.name.orEmpty(),
        workspaceFileScope = workspace?.fileScope.orEmpty(),
        workspacePermission = workspace?.permissionLevel,
        workspaceCapabilities = workspace?.capabilities ?: persistentSetOf(),
    )

internal fun nodeMatchesQuery(
    node: NodeDescriptor,
    query: String,
): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    val nodeMatch =
        node.name.contains(needle, ignoreCase = true) ||
            node.relayName.contains(needle, ignoreCase = true) ||
            node.platform.contains(needle, ignoreCase = true)
    return nodeMatch || node.workspaces.any { workspaceMatches(it, needle) }
}

internal fun visibleWorkspacesForNode(
    node: NodeDescriptor,
    query: String,
): List<WorkspaceDescriptor> {
    val needle = query.trim()
    val nodeHeaderMatched =
        needle.isEmpty() ||
            node.name.contains(needle, ignoreCase = true) ||
            node.relayName.contains(needle, ignoreCase = true)
    return if (nodeHeaderMatched) {
        node.workspaces
    } else {
        node.workspaces.filter { workspaceMatches(it, needle) }
    }
}

private fun workspaceMatches(
    workspace: WorkspaceDescriptor,
    needle: String,
): Boolean =
    workspace.name.contains(needle, ignoreCase = true) ||
        workspace.fileScope.contains(needle, ignoreCase = true) ||
        workspace.capabilities.any { it.name.contains(needle, ignoreCase = true) }

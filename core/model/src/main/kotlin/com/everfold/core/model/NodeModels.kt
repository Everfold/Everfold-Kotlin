package com.everfold.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
enum class NodeRole {
    AUTHORITY,
    WORKER,
}

@Serializable
enum class NodeStatus {
    ONLINE,
    DEGRADED,
    OFFLINE,
}

@Serializable
enum class NodeCapability {
    LOOP_HOST,
    LLM_GATEWAY,
    TOOL_RUNTIME,
    MCP_BRIDGE,
    CODE_MODE,
}

@Serializable
enum class WorkspacePermissionLevel {
    READ_ONLY,
    WORKSPACE_WRITE,
    FULL_ACCESS,
}

@Serializable
enum class WorkspaceCapability {
    SHELL,
    FILE_EDIT,
    MCP,
    CODE_MODE,
    NETWORK,
}

data class WorkspaceDescriptor(
    val id: String,
    val name: String,
    val fileScope: String,
    val permissionLevel: WorkspacePermissionLevel,
    val capabilities: ImmutableSet<WorkspaceCapability>,
    val description: String = "",
)

data class NodeDescriptor(
    val id: String,
    val name: String,
    val platform: String,
    val roles: ImmutableSet<NodeRole>,
    val status: NodeStatus,
    val capabilities: ImmutableSet<NodeCapability>,
    val latencyMs: Int?,
    val isLocalDevice: Boolean = false,
    val relayProfileId: String? = null,
    val relayName: String = "",
    val workspaces: ImmutableList<WorkspaceDescriptor> = persistentListOf(),
)

@Serializable
data class NodeAttribution(
    val nodeId: String,
    val nodeName: String,
    val platform: String,
)

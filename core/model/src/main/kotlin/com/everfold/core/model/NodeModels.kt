package com.everfold.core.model

import kotlinx.collections.immutable.ImmutableSet
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

data class NodeDescriptor(
    val id: String,
    val name: String,
    val platform: String,
    val roles: ImmutableSet<NodeRole>,
    val status: NodeStatus,
    val capabilities: ImmutableSet<NodeCapability>,
    val latencyMs: Int?,
    val isLocalDevice: Boolean = false,
)

@Serializable
data class NodeAttribution(
    val nodeId: String,
    val nodeName: String,
    val platform: String,
)

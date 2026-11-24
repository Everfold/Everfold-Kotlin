package com.everfold.core.gateway.mock

import com.everfold.core.model.ActionKind
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.ActionTrace
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.LoopState
import com.everfold.core.model.ModelCapability
import com.everfold.core.model.ModelDescriptor
import com.everfold.core.model.NodeAttribution
import com.everfold.core.model.NodeCapability
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.NodeRole
import com.everfold.core.model.NodeStatus
import com.everfold.core.model.ObservedItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal object MockFixtures {
    private const val CLOUD_LATENCY_MS = 28
    private const val DESKTOP_LATENCY_MS = 14
    private const val LOCAL_LATENCY_MS = 1
    private const val SAMPLE_MCP_DURATION_MS = 420L
    private const val SAMPLE_CODE_DURATION_MS = 680L

    val cloudAttribution =
        NodeAttribution(
            nodeId = "node-authority-cloud",
            nodeName = "Cloud Authority",
            platform = "Linux Server",
        )

    val desktopAttribution =
        NodeAttribution(
            nodeId = "node-worker-desktop",
            nodeName = "Workstation",
            platform = "Linux Desktop",
        )

    val localAttribution =
        NodeAttribution(
            nodeId = "node-local-android",
            nodeName = "本机 Android",
            platform = "Android",
        )

    fun nodesFor(mode: ConnectionMode): ImmutableList<NodeDescriptor> =
        when (mode) {
            ConnectionMode.LOCAL_ONLY -> persistentListOf(localAuthorityNode())
            ConnectionMode.REMOTE_AUTHORITY, ConnectionMode.RELAY -> remoteClusterNodes()
        }

    fun initialModels(): ImmutableList<ModelDescriptor> =
        persistentListOf(
            ModelDescriptor(
                id = "model-claude-sonnet",
                displayName = "Claude Sonnet 4.6",
                provider = "Anthropic",
                contextWindowLabel = "200K",
                hostNodeName = cloudAttribution.nodeName,
                capabilities =
                    persistentSetOf(
                        ModelCapability.REASONING,
                        ModelCapability.TOOL_USE,
                        ModelCapability.VISION,
                        ModelCapability.CODE_MODE,
                    ),
                isDefault = true,
            ),
            ModelDescriptor(
                id = "model-gemini-pro",
                displayName = "Gemini 3 Pro",
                provider = "Google",
                contextWindowLabel = "1M",
                hostNodeName = cloudAttribution.nodeName,
                capabilities =
                    persistentSetOf(
                        ModelCapability.REASONING,
                        ModelCapability.TOOL_USE,
                        ModelCapability.VISION,
                    ),
            ),
            ModelDescriptor(
                id = "model-local-qwen",
                displayName = "Qwen3-8B (Local Preview)",
                provider = "On-Device",
                contextWindowLabel = "32K",
                hostNodeName = localAttribution.nodeName,
                capabilities = persistentSetOf(ModelCapability.TOOL_USE),
            ),
        )

    fun initialSessions(mode: ConnectionMode): List<AgentSessionDetail> {
        val loopHost =
            if (mode == ConnectionMode.LOCAL_ONLY) {
                localAttribution.nodeName
            } else {
                cloudAttribution.nodeName
            }
        val welcomeSession =
            AgentSessionDetail(
                summary =
                    AgentSessionSummary(
                        id = "session-welcome",
                        title = "多节点架构与项目状态概览",
                        modelId = "model-claude-sonnet",
                        loopState = LoopState.COMPLETED,
                        loopHostNodeName = loopHost,
                        lastMessagePreview = "已完成跨节点动作检查，当前拓扑健康。",
                        updatedAtLabel = "刚刚",
                        workspaceName = "Everfold-Kotlin",
                    ),
                items = welcomeItems(mode = mode, loopHost = loopHost),
            )
        return listOf(welcomeSession) + MockSessionFixtures.additionalSessions(mode = mode, loopHost = loopHost)
    }

    private fun localAuthorityNode(): NodeDescriptor =
        NodeDescriptor(
            id = localAttribution.nodeId,
            name = localAttribution.nodeName,
            platform = localAttribution.platform,
            roles = persistentSetOf(NodeRole.AUTHORITY, NodeRole.WORKER),
            status = NodeStatus.ONLINE,
            capabilities = persistentSetOf(NodeCapability.LOOP_HOST, NodeCapability.TOOL_RUNTIME),
            latencyMs = LOCAL_LATENCY_MS,
            isLocalDevice = true,
        )

    private fun remoteClusterNodes(): ImmutableList<NodeDescriptor> =
        persistentListOf(
            NodeDescriptor(
                id = cloudAttribution.nodeId,
                name = cloudAttribution.nodeName,
                platform = cloudAttribution.platform,
                roles = persistentSetOf(NodeRole.AUTHORITY, NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities =
                    persistentSetOf(
                        NodeCapability.LOOP_HOST,
                        NodeCapability.LLM_GATEWAY,
                        NodeCapability.CODE_MODE,
                    ),
                latencyMs = CLOUD_LATENCY_MS,
            ),
            NodeDescriptor(
                id = desktopAttribution.nodeId,
                name = desktopAttribution.nodeName,
                platform = desktopAttribution.platform,
                roles = persistentSetOf(NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities =
                    persistentSetOf(
                        NodeCapability.TOOL_RUNTIME,
                        NodeCapability.MCP_BRIDGE,
                        NodeCapability.CODE_MODE,
                    ),
                latencyMs = DESKTOP_LATENCY_MS,
            ),
            NodeDescriptor(
                id = localAttribution.nodeId,
                name = localAttribution.nodeName,
                platform = localAttribution.platform,
                roles = persistentSetOf(NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities = persistentSetOf(NodeCapability.TOOL_RUNTIME),
                latencyMs = LOCAL_LATENCY_MS,
                isLocalDevice = true,
            ),
        )

    private fun welcomeItems(
        mode: ConnectionMode,
        loopHost: String,
    ): ImmutableList<ObservedItem> {
        val firstNode =
            if (mode == ConnectionMode.LOCAL_ONLY) localAttribution else desktopAttribution
        val secondNode =
            if (mode == ConnectionMode.LOCAL_ONLY) localAttribution else cloudAttribution
        val actions =
            persistentListOf(
                ActionTrace(
                    id = "act-1",
                    kind = ActionKind.MCP_CALL,
                    title = "MCP · git.status",
                    summary = "读取工作区变更与分支状态",
                    status = ActionStatus.SUCCEEDED,
                    durationMs = SAMPLE_MCP_DURATION_MS,
                    executedOn = firstNode,
                ),
                ActionTrace(
                    id = "act-2",
                    kind = ActionKind.CODE_MODE,
                    title = "CodeMode · topology_probe",
                    summary = "汇总各 Worker 节点的执行能力与延迟",
                    status = ActionStatus.SUCCEEDED,
                    durationMs = SAMPLE_CODE_DURATION_MS,
                    executedOn = secondNode,
                ),
            )
        return persistentListOf(
            ObservedItem.UserMessage(
                id = "msg-u-1",
                content = "帮我检查当前仓库状态，并确认可用节点能力。",
                timestampLabel = "刚刚",
            ),
            ObservedItem.ActionGroup(id = "act-g-1", actions = actions),
            ObservedItem.AssistantMessage(
                id = "msg-a-1",
                markdown =
                    """
                    已通过 **Authority** 协调完成本次检查：

                    - **Loop 宿主**：`$loopHost`
                    - **动作执行**：MCP 工具与代码分析已透明调度至具备能力的 Worker 节点
                    - **下一步**：直接在下方输入指令即可观察新的多步骤流式执行过程。
                    """.trimIndent(),
                isStreaming = false,
                timestampLabel = "刚刚",
            ),
        )
    }
}

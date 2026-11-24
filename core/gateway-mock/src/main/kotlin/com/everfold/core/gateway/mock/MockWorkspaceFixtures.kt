package com.everfold.core.gateway.mock

import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.NodeCapability
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.NodeRole
import com.everfold.core.model.NodeStatus
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.WorkspaceCapability
import com.everfold.core.model.WorkspaceDescriptor
import com.everfold.core.model.WorkspacePermissionLevel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal object MockWorkspaceFixtures {
    private const val CLOUD_LATENCY_MS = 28
    private const val DESKTOP_LATENCY_MS = 14
    private const val EDGE_PI_LATENCY_MS = 9
    private const val STUDIO_MAC_LATENCY_MS = 6
    private const val CUSTOM_LATENCY_MS = 22
    private const val LOCAL_LATENCY_MS = 1

    val everfoldCloudWorkspace =
        WorkspaceDescriptor(
            id = "ws-cloud-everfold",
            name = "Everfold-Kotlin",
            fileScope = "/srv/repos/everfold-kotlin",
            permissionLevel = WorkspacePermissionLevel.WORKSPACE_WRITE,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.FILE_EDIT,
                    WorkspaceCapability.MCP,
                    WorkspaceCapability.CODE_MODE,
                ),
            description = "主仓库源码读写与 MCP/CodeMode 构建验证区",
        )

    val infraOpsWorkspace =
        WorkspaceDescriptor(
            id = "ws-cloud-infra",
            name = "Infra Ops",
            fileScope = "/srv/infra/cluster-ops",
            permissionLevel = WorkspacePermissionLevel.FULL_ACCESS,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.SHELL,
                    WorkspaceCapability.FILE_EDIT,
                    WorkspaceCapability.MCP,
                    WorkspaceCapability.NETWORK,
                ),
            description = "生产运维脚本、集群配置与外网探测权限",
        )

    val specResearchWorkspace =
        WorkspaceDescriptor(
            id = "ws-cloud-spec",
            name = "Spec Research",
            fileScope = "/srv/docs/specs",
            permissionLevel = WorkspacePermissionLevel.READ_ONLY,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.MCP,
                    WorkspaceCapability.NETWORK,
                ),
            description = "架构文档只读检索，禁止任何文件写入",
        )

    val workstationEverfoldWorkspace =
        WorkspaceDescriptor(
            id = "ws-workstation-everfold",
            name = "Everfold-Kotlin",
            fileScope = "/home/dev/Everfold-Kotlin",
            permissionLevel = WorkspacePermissionLevel.FULL_ACCESS,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.SHELL,
                    WorkspaceCapability.FILE_EDIT,
                    WorkspaceCapability.MCP,
                    WorkspaceCapability.CODE_MODE,
                    WorkspaceCapability.NETWORK,
                ),
            description = "开发工作站完整编译、测试与 Git 操作环境",
        )

    val workstationSandboxWorkspace =
        WorkspaceDescriptor(
            id = "ws-workstation-sandbox",
            name = "Sandbox Lab",
            fileScope = "/tmp/everfold-sandbox",
            permissionLevel = WorkspacePermissionLevel.WORKSPACE_WRITE,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.SHELL,
                    WorkspaceCapability.CODE_MODE,
                ),
            description = "隔离沙箱目录，仅可读写临时实验文件",
        )

    val edgeHomeLabWorkspace =
        WorkspaceDescriptor(
            id = "ws-edge-homelab",
            name = "Home Lab",
            fileScope = "/opt/homelab/services",
            permissionLevel = WorkspacePermissionLevel.WORKSPACE_WRITE,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.SHELL,
                    WorkspaceCapability.FILE_EDIT,
                    WorkspaceCapability.NETWORK,
                ),
            description = "边缘网关容器编排与服务配置目录",
        )

    val edgeSensorLogsWorkspace =
        WorkspaceDescriptor(
            id = "ws-edge-sensors",
            name = "Sensor Logs",
            fileScope = "/var/log/sensors",
            permissionLevel = WorkspacePermissionLevel.READ_ONLY,
            capabilities = persistentSetOf(WorkspaceCapability.MCP),
            description = "只读传感器日志归档区",
        )

    val studioMacWorkspace =
        WorkspaceDescriptor(
            id = "ws-studio-everfold",
            name = "Everfold-Kotlin",
            fileScope = "/Users/dev/Everfold-Kotlin",
            permissionLevel = WorkspacePermissionLevel.WORKSPACE_WRITE,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.SHELL,
                    WorkspaceCapability.FILE_EDIT,
                    WorkspaceCapability.CODE_MODE,
                ),
            description = "Apple Silicon 本地同步镜像仓库",
        )

    val localAndroidWorkspace =
        WorkspaceDescriptor(
            id = "ws-local-android",
            name = "Android Workspace",
            fileScope = "/storage/emulated/0/Documents/Everfold",
            permissionLevel = WorkspacePermissionLevel.WORKSPACE_WRITE,
            capabilities =
                persistentSetOf(
                    WorkspaceCapability.FILE_EDIT,
                    WorkspaceCapability.NETWORK,
                ),
            description = "本机应用文档工作区与离线资料库",
        )

    val localDownloadsWorkspace =
        WorkspaceDescriptor(
            id = "ws-local-downloads",
            name = "Downloads",
            fileScope = "/storage/emulated/0/Download",
            permissionLevel = WorkspacePermissionLevel.READ_ONLY,
            capabilities = persistentSetOf(WorkspaceCapability.FILE_EDIT),
            description = "本机下载目录只读访问",
        )

    fun cloudWorkspaces(): ImmutableList<WorkspaceDescriptor> =
        persistentListOf(everfoldCloudWorkspace, infraOpsWorkspace, specResearchWorkspace)

    fun workstationWorkspaces(): ImmutableList<WorkspaceDescriptor> =
        persistentListOf(workstationEverfoldWorkspace, workstationSandboxWorkspace)

    fun localWorkspaces(): ImmutableList<WorkspaceDescriptor> = persistentListOf(localAndroidWorkspace, localDownloadsWorkspace)

    fun nodesForProfile(profile: ConnectionProfile): List<NodeDescriptor> =
        when (profile.id) {
            "profile-nats-cloud" -> natsCloudNodes(profile)
            "profile-zenoh-p2p" -> zenohP2pNodes(profile)
            else -> customProfileNodes(profile)
        }

    fun localNodes(): List<NodeDescriptor> =
        listOf(
            NodeDescriptor(
                id = MockFixtures.localAttribution.nodeId,
                name = MockFixtures.localAttribution.nodeName,
                platform = MockFixtures.localAttribution.platform,
                roles = persistentSetOf(NodeRole.AUTHORITY, NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities = persistentSetOf(NodeCapability.LOOP_HOST, NodeCapability.TOOL_RUNTIME),
                latencyMs = LOCAL_LATENCY_MS,
                isLocalDevice = true,
                relayProfileId = LOCAL_RELAY_ID,
                relayName = "本机运行模式",
                workspaces = localWorkspaces(),
            ),
        )

    fun buildRuntimeContext(
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

    private fun natsCloudNodes(profile: ConnectionProfile): List<NodeDescriptor> =
        listOf(
            NodeDescriptor(
                id = MockFixtures.cloudAttribution.nodeId,
                name = MockFixtures.cloudAttribution.nodeName,
                platform = MockFixtures.cloudAttribution.platform,
                roles = persistentSetOf(NodeRole.AUTHORITY, NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities =
                    persistentSetOf(
                        NodeCapability.LOOP_HOST,
                        NodeCapability.LLM_GATEWAY,
                        NodeCapability.CODE_MODE,
                    ),
                latencyMs = CLOUD_LATENCY_MS,
                relayProfileId = profile.id,
                relayName = profile.name,
                workspaces = cloudWorkspaces(),
            ),
            NodeDescriptor(
                id = MockFixtures.desktopAttribution.nodeId,
                name = MockFixtures.desktopAttribution.nodeName,
                platform = MockFixtures.desktopAttribution.platform,
                roles = persistentSetOf(NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities =
                    persistentSetOf(
                        NodeCapability.TOOL_RUNTIME,
                        NodeCapability.MCP_BRIDGE,
                        NodeCapability.CODE_MODE,
                    ),
                latencyMs = DESKTOP_LATENCY_MS,
                relayProfileId = profile.id,
                relayName = profile.name,
                workspaces = workstationWorkspaces(),
            ),
        )

    private fun zenohP2pNodes(profile: ConnectionProfile): List<NodeDescriptor> =
        listOf(
            NodeDescriptor(
                id = "node-zenoh-edge-pi",
                name = "Edge Pi 5",
                platform = "Linux ARM64",
                roles = persistentSetOf(NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities =
                    persistentSetOf(
                        NodeCapability.LOOP_HOST,
                        NodeCapability.TOOL_RUNTIME,
                        NodeCapability.MCP_BRIDGE,
                    ),
                latencyMs = EDGE_PI_LATENCY_MS,
                relayProfileId = profile.id,
                relayName = profile.name,
                workspaces = persistentListOf(edgeHomeLabWorkspace, edgeSensorLogsWorkspace),
            ),
            NodeDescriptor(
                id = "node-zenoh-studio-mac",
                name = "Studio Mac",
                platform = "macOS",
                roles = persistentSetOf(NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities =
                    persistentSetOf(
                        NodeCapability.LOOP_HOST,
                        NodeCapability.TOOL_RUNTIME,
                        NodeCapability.CODE_MODE,
                    ),
                latencyMs = STUDIO_MAC_LATENCY_MS,
                relayProfileId = profile.id,
                relayName = profile.name,
                workspaces = persistentListOf(studioMacWorkspace),
            ),
        )

    private fun customProfileNodes(profile: ConnectionProfile): List<NodeDescriptor> =
        listOf(
            NodeDescriptor(
                id = "node-${profile.id}",
                name = "${profile.name} Node",
                platform = "Linux",
                roles = persistentSetOf(NodeRole.AUTHORITY, NodeRole.WORKER),
                status = NodeStatus.ONLINE,
                capabilities =
                    persistentSetOf(
                        NodeCapability.LOOP_HOST,
                        NodeCapability.LLM_GATEWAY,
                        NodeCapability.TOOL_RUNTIME,
                        NodeCapability.MCP_BRIDGE,
                    ),
                latencyMs = CUSTOM_LATENCY_MS,
                relayProfileId = profile.id,
                relayName = profile.name,
                workspaces =
                    persistentListOf(
                        WorkspaceDescriptor(
                            id = "ws-${profile.id}-default",
                            name = "Default Workspace",
                            fileScope = "/workspace",
                            permissionLevel = WorkspacePermissionLevel.WORKSPACE_WRITE,
                            capabilities =
                                persistentSetOf(
                                    WorkspaceCapability.SHELL,
                                    WorkspaceCapability.FILE_EDIT,
                                    WorkspaceCapability.MCP,
                                ),
                            description = "节点默认隔离工作区",
                        ),
                    ),
            ),
        )

    private const val LOCAL_RELAY_ID = "local"
}

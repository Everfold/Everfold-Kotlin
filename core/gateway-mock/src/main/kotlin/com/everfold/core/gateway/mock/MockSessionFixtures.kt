package com.everfold.core.gateway.mock

import com.everfold.core.model.ActionKind
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.ActionTrace
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import kotlinx.collections.immutable.persistentListOf

internal object MockSessionFixtures {
    private const val SAMPLE_GATEWAY_MS = 260L
    private const val SAMPLE_PERF_MS = 510L
    private const val SAMPLE_SANDBOX_MS = 390L

    fun additionalSessions(
        mode: ConnectionMode,
        loopHost: String,
    ): List<AgentSessionDetail> =
        listOf(
            gatewayRefactorSession(mode),
            composePerfSession(mode),
            mcpSandboxSession(mode = mode, loopHost = loopHost),
            legacyMigrationSession(loopHost = loopHost),
        )

    private fun gatewayRefactorSession(mode: ConnectionMode): AgentSessionDetail {
        val nodeAttribution =
            if (mode == ConnectionMode.LOCAL_ONLY) {
                MockFixtures.localAttribution
            } else {
                MockFixtures.desktopAttribution
            }
        return AgentSessionDetail(
            summary =
                AgentSessionSummary(
                    id = "session-gateway-refactor",
                    title = "排查 Gateway 重连超时与心跳抖动",
                    modelId = "model-claude-sonnet",
                    loopState = LoopState.AWAITING_INPUT,
                    loopHostNodeName = nodeAttribution.nodeName,
                    lastMessagePreview = "已定位 WebSocket 心跳退避策略偏差，等待确认重连阈值。",
                    updatedAtLabel = "12 分钟前",
                    workspaceName = "Infra Ops",
                ),
            items =
                persistentListOf(
                    ObservedItem.UserMessage(
                        id = "msg-gw-u-1",
                        content = "弱网切换时 Gateway 长连接经常卡在重连阶段，帮我检查心跳和退避逻辑。",
                        timestampLabel = "15 分钟前",
                    ),
                    ObservedItem.ActionGroup(
                        id = "act-gw-g-1",
                        actions =
                            persistentListOf(
                                ActionTrace(
                                    id = "act-gw-1",
                                    kind = ActionKind.MCP_CALL,
                                    title = "MCP · logs.inspect",
                                    summary = "分析断线重连时序日志 trace-only-token-99",
                                    status = ActionStatus.SUCCEEDED,
                                    durationMs = SAMPLE_GATEWAY_MS,
                                    executedOn = nodeAttribution,
                                ),
                            ),
                    ),
                    ObservedItem.AssistantMessage(
                        id = "msg-gw-a-1",
                        markdown =
                            "已排查 WebSocket 心跳与退避策略：\n\n" +
                                "- 当前指数退避未重置抖动上限，建议将最大重试间隔收紧至 15s。\n" +
                                "- 请确认是否允许在断网恢复时立即触发一次轻量探活。",
                        timestampLabel = "12 分钟前",
                    ),
                ),
        )
    }

    private fun composePerfSession(mode: ConnectionMode): AgentSessionDetail {
        val workerNode =
            if (mode == ConnectionMode.LOCAL_ONLY) {
                MockFixtures.localAttribution
            } else {
                MockFixtures.desktopAttribution
            }
        return AgentSessionDetail(
            summary =
                AgentSessionSummary(
                    id = "session-compose-perf",
                    title = "优化 Compose 列表滚动与动画帧率",
                    modelId = "model-gemini-pro",
                    loopState = LoopState.IDLE,
                    loopHostNodeName = MockFixtures.localAttribution.nodeName,
                    lastMessagePreview = "建议固定 LazyColumn key 并统一 MotionDurationScale 缩放。",
                    updatedAtLabel = "2 小时前",
                    workspaceName = "Everfold-Kotlin",
                ),
            items =
                persistentListOf(
                    ObservedItem.UserMessage(
                        id = "msg-perf-u-1",
                        content = "抽屉切换和消息长列表快速滑动时有轻微掉帧，帮我查下重组热点。",
                        timestampLabel = "2 小时前",
                    ),
                    ObservedItem.ActionGroup(
                        id = "act-perf-g-1",
                        actions =
                            persistentListOf(
                                ActionTrace(
                                    id = "act-perf-1",
                                    kind = ActionKind.CODE_MODE,
                                    title = "CodeMode · compose_metrics",
                                    summary = "检查 LazyColumn 稳定性与重组次数",
                                    status = ActionStatus.SUCCEEDED,
                                    durationMs = SAMPLE_PERF_MS,
                                    executedOn = workerNode,
                                ),
                            ),
                    ),
                    ObservedItem.AssistantMessage(
                        id = "msg-perf-a-1",
                        markdown =
                            "定位到两处优化点：\n\n" +
                                "1. 为 `LazyColumn` 所有 item 显式提供稳定的 `key`，避免重组抖动。\n" +
                                "2. 动画统一接入 `MotionDurationScale`，跟随全局动画速度配置。",
                        timestampLabel = "2 小时前",
                    ),
                ),
        )
    }

    private fun mcpSandboxSession(
        mode: ConnectionMode,
        loopHost: String,
    ): AgentSessionDetail {
        val actionNode =
            if (mode == ConnectionMode.LOCAL_ONLY) {
                MockFixtures.localAttribution
            } else {
                MockFixtures.cloudAttribution
            }
        return AgentSessionDetail(
            summary =
                AgentSessionSummary(
                    id = "session-mcp-sandbox",
                    title = "验证代码沙箱权限边界与失败回滚",
                    modelId = "model-claude-sonnet",
                    loopState = LoopState.FAILED,
                    loopHostNodeName = loopHost,
                    lastMessagePreview = "沙箱隔离测试在只读目录校验时触发权限拒绝，已保留现场。",
                    updatedAtLabel = "昨天",
                    workspaceName = "Spec Research",
                ),
            items =
                persistentListOf(
                    ObservedItem.UserMessage(
                        id = "msg-sb-u-1",
                        content = "请跑一遍沙箱文件权限回归，确认越界写入会被拦截并正确回滚。",
                        timestampLabel = "昨天",
                    ),
                    ObservedItem.ActionGroup(
                        id = "act-sb-g-1",
                        actions =
                            persistentListOf(
                                ActionTrace(
                                    id = "act-sb-1",
                                    kind = ActionKind.TOOL_CALL,
                                    title = "Tool · sandbox.verify",
                                    summary = "执行只读工作区越界写入与回滚测试",
                                    status = ActionStatus.FAILED,
                                    durationMs = SAMPLE_SANDBOX_MS,
                                    executedOn = actionNode,
                                ),
                            ),
                    ),
                    ObservedItem.AssistantMessage(
                        id = "msg-sb-a-1",
                        markdown =
                            "沙箱权限回归检测发现异常：\n\n" +
                                "- 越界写入已被内核沙箱成功拦截。\n" +
                                "- 但临时工作区的快照回滚脚本因只读挂载未完成清理。",
                        timestampLabel = "昨天",
                    ),
                ),
        )
    }

    private fun legacyMigrationSession(loopHost: String): AgentSessionDetail =
        AgentSessionDetail(
            summary =
                AgentSessionSummary(
                    id = "session-legacy-migration",
                    title = "旧版配置 Schema 迁移记录",
                    modelId = "model-claude-sonnet",
                    loopState = LoopState.COMPLETED,
                    loopHostNodeName = loopHost,
                    lastMessagePreview = "已完成旧版 YAML 到模块配置的结构转换，归档备查。",
                    updatedAtLabel = "3 天前",
                    workspaceName = "Infra Ops",
                    isArchived = true,
                ),
            items =
                persistentListOf(
                    ObservedItem.UserMessage(
                        id = "msg-mig-u-1",
                        content = "请检查旧版配置 Schema 迁移日志，确认所有历史字段已兼容。",
                        timestampLabel = "3 天前",
                    ),
                    ObservedItem.AssistantMessage(
                        id = "msg-mig-a-1",
                        markdown = "迁移检查通过，所有历史配置字段均已映射到新结构，可安全归档保留。",
                        timestampLabel = "3 天前",
                    ),
                ),
        )
}

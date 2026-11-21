package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.conversation.AgentStreamBlock
import com.everfold.kotlin.feature.conversation.StreamRunStatus
import com.everfold.kotlin.feature.conversation.TraceSpan
import com.everfold.kotlin.feature.conversation.TraceSpanKind

private val StorageKeywords =
    listOf("download", "screenshot", "apk", "storage", "archive", "clean", "整理", "下载", "截图", "清理", "存储", "归档", "重复")
private val DiagnosticsKeywords =
    listOf("log", "crash", "anr", "battery", "drain", "diagnos", "power", "日志", "崩溃", "耗电", "诊断", "卡顿", "发热", "省电")
private val IndexKeywords =
    listOf("index", "search", "offline", "fts", "document", "索引", "离线", "检索", "知识库", "文档")

private const val PRESEEDED_STAGE_INDEX = 6
private const val PRESEEDED_CHUNK_OFFSET = 14
private const val PRESEEDED_STAGE_ELAPSED_MS = 7_000L
private const val PRESEEDED_PRIOR_ELAPSED_MS = 25_000L
private const val PRESEEDED_TOTAL_ELAPSED_MS = 32_000L

internal const val PAUSED_SAMPLE_CONVERSATION_ID = "android-storage-audit"
internal const val PAUSED_SAMPLE_ASSISTANT_MESSAGE_ID = "android-storage-audit-assistant"

internal object OnDeviceScenarioCatalog {
    fun selectScenario(
        prompt: String,
        connectionId: String,
        workspacePath: String,
        hasAttachments: Boolean,
    ): ScenarioSpec {
        if (connectionId != BUILT_IN_CONNECTION_ID) {
            return buildGenericWorkspaceScenario(workspacePath)
        }
        val normalized = prompt.lowercase()
        return when {
            StorageKeywords.any { it in normalized } -> buildStorageArchiveScenario()
            DiagnosticsKeywords.any { it in normalized } -> buildDiagnosticsScenario()
            hasAttachments || IndexKeywords.any { it in normalized } -> buildDocumentIndexScenario(workspacePath)
            else -> buildGenericWorkspaceScenario(workspacePath)
        }
    }

    fun preseededPausedStorageRun(): TaskRunRecord {
        val scenario = buildStorageArchiveScenario()
        val prompt = "深度整理本机 Download 与截图目录并清理重复安装包"
        val context = TaskRunContext(prompt, "/workspace", emptyList(), null, emptyList(), PRESEEDED_TOTAL_ELAPSED_MS)
        val completedStages = scenario.stages.take(PRESEEDED_STAGE_INDEX)
        val completedBlocks = completedStages.mapNotNull { stage -> completedBlockForStage(stage, context) }
        val completedSpans = buildCompletedSpansForStages(completedStages, context)
        val hashStage = scenario.stages[PRESEEDED_STAGE_INDEX] as StreamStageSpec.ToolStage
        val partialLogs = (1..PRESEEDED_CHUNK_OFFSET).mapNotNull { chunk -> hashStage.logLineAtChunk(chunk, hashStage.totalChunks) }
        val pausedTool =
            AgentStreamBlock.ToolCall(
                id = hashStage.id,
                toolName = hashStage.toolName,
                command = hashStage.command,
                status = StreamRunStatus.Paused,
                summary = hashStage.runningSummary(PRESEEDED_CHUNK_OFFSET, hashStage.totalItems),
                logLines = partialLogs,
                progress = PRESEEDED_CHUNK_OFFSET.toFloat() / hashStage.totalChunks,
                durationMillis = PRESEEDED_STAGE_ELAPSED_MS,
            )
        val pausedSpan =
            TraceSpan(
                id = hashStage.id,
                kind = TraceSpanKind.ToolCall,
                title = hashStage.toolName,
                detail = hashStage.command,
                startOffsetMs = PRESEEDED_PRIOR_ELAPSED_MS,
                endOffsetMs = PRESEEDED_TOTAL_ELAPSED_MS,
                status = StreamRunStatus.Paused,
            )
        return TaskRunRecord(
            taskId = "task-preseeded-storage",
            conversationId = PAUSED_SAMPLE_CONVERSATION_ID,
            connectionId = BUILT_IN_CONNECTION_ID,
            assistantMessageId = PAUSED_SAMPLE_ASSISTANT_MESSAGE_ID,
            prompt = prompt,
            workspacePath = "/workspace",
            attachmentNames = emptyList(),
            scenario = scenario,
            committedBlocks = completedBlocks,
            activeBlock = pausedTool,
            committedSpans = completedSpans,
            activeSpan = pausedSpan,
            currentStageIndex = PRESEEDED_STAGE_INDEX,
            currentChunkOffset = PRESEEDED_CHUNK_OFFSET,
            currentStageElapsedMs = PRESEEDED_STAGE_ELAPSED_MS,
            activeLogLines = partialLogs,
            elapsedMillis = PRESEEDED_TOTAL_ELAPSED_MS,
        )
    }
}

private fun buildCompletedSpansForStages(
    stages: List<StreamStageSpec>,
    context: TaskRunContext,
): List<TraceSpan> {
    var cursorMs = 0L
    return stages.mapNotNull { stage ->
        val span =
            when (stage) {
                is StreamStageSpec.ThinkStage -> {
                    TraceSpan(
                        id = stage.id,
                        kind = TraceSpanKind.LlmRequest,
                        title = textStageSpanTitle(stage),
                        detail = "${stage.thought.length} chars",
                        startOffsetMs = cursorMs,
                        endOffsetMs = cursorMs + stage.durationMillis,
                        ttftMillis = (stage.durationMillis / stage.totalUnits.coerceAtLeast(1)).coerceAtLeast(1L),
                    )
                }

                is StreamStageSpec.NarrationStage -> {
                    val text = stage.textBuilder(context)
                    TraceSpan(
                        id = stage.id,
                        kind = TraceSpanKind.LlmRequest,
                        title = textStageSpanTitle(stage),
                        detail = "${text.length} chars",
                        startOffsetMs = cursorMs,
                        endOffsetMs = cursorMs + stage.durationMillis,
                        ttftMillis = (stage.durationMillis / stage.totalUnits.coerceAtLeast(1)).coerceAtLeast(1L),
                    )
                }

                is StreamStageSpec.ToolStage -> {
                    val dur = stage.totalChunks * stage.chunkDelayMillis
                    TraceSpan(
                        id = stage.id,
                        kind = TraceSpanKind.ToolCall,
                        title = stage.toolName,
                        detail = stage.command,
                        startOffsetMs = cursorMs,
                        endOffsetMs = cursorMs + dur,
                    )
                }

                is StreamStageSpec.ApprovalStage -> {
                    null
                }
            }
        cursorMs = span?.endOffsetMs ?: cursorMs
        span
    }
}

private fun completedBlockForStage(
    stage: StreamStageSpec,
    context: TaskRunContext,
): AgentStreamBlock? =
    when (stage) {
        is StreamStageSpec.ThinkStage -> {
            AgentStreamBlock.Thinking(
                id = stage.id,
                text = stage.thought,
                status = StreamRunStatus.Completed,
                durationMillis = stage.durationMillis,
            )
        }

        is StreamStageSpec.NarrationStage -> {
            AgentStreamBlock.Narration(
                id = stage.id,
                text = stage.textBuilder(context),
                isStreaming = false,
                isFinalAnswer = stage.isFinalAnswer,
            )
        }

        is StreamStageSpec.ToolStage -> {
            val logs = (1..stage.totalChunks).mapNotNull { chunk -> stage.logLineAtChunk(chunk, stage.totalChunks) }
            AgentStreamBlock.ToolCall(
                id = stage.id,
                toolName = stage.toolName,
                command = stage.command,
                status = StreamRunStatus.Completed,
                summary = stage.completedSummary,
                logLines = logs,
                progress = 1f,
                durationMillis = stage.totalChunks * stage.chunkDelayMillis,
            )
        }

        is StreamStageSpec.ApprovalStage -> {
            null
        }
    }

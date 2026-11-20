package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.scenario_generic_headline
import everfold.shared.generated.resources.scenario_generic_scan_done
import everfold.shared.generated.resources.scenario_generic_scan_running
import everfold.shared.generated.resources.scenario_generic_verify_done
import everfold.shared.generated.resources.scenario_generic_verify_running
import everfold.shared.generated.resources.scenario_index_chunk_done
import everfold.shared.generated.resources.scenario_index_chunk_running
import everfold.shared.generated.resources.scenario_index_commit_done
import everfold.shared.generated.resources.scenario_index_commit_running
import everfold.shared.generated.resources.scenario_index_diff_done
import everfold.shared.generated.resources.scenario_index_diff_running
import everfold.shared.generated.resources.scenario_index_headline

private const val CHUNK_DELAY_MS = 500L
private const val MILLIS_PER_SEC = 1_000L

private const val INDEX_THOUGHT_1_MS = 2_500L
private const val INDEX_INTRO_MS = 1_500L
private const val INDEX_DIFF_CHUNKS = 8
private const val INDEX_AFTER_DIFF_MS = 2_000L
private const val INDEX_PARSE_CHUNKS = 30
private const val INDEX_DOC_COUNT = 64
private const val INDEX_THOUGHT_2_MS = 2_000L
private const val INDEX_COMMIT_CHUNKS = 34
private const val INDEX_PAGE_BATCHES = 48
private const val INDEX_THERMAL_CHUNK = 17
private const val INDEX_SUMMARY_MS = 7_000L

private const val GENERIC_THOUGHT_1_MS = 3_000L
private const val GENERIC_INTRO_MS = 2_000L
private const val GENERIC_SCAN_CHUNKS = 28
private const val GENERIC_CONTEXT_ITEMS = 42
private const val GENERIC_THOUGHT_2_MS = 2_500L
private const val GENERIC_VERIFY_CHUNKS = 36
private const val GENERIC_CHECK_RULES = 12
private const val GENERIC_SUMMARY_MS = 10_000L

private const val LOG_STRIDE_SEVEN = 7
private const val LOG_STRIDE_EIGHT = 8
private const val LOG_CHUNK_FOUR = 4
private const val PASSAGES_TOTAL = 920

internal fun buildDocumentIndexScenario(workspacePath: String): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.DocumentIndex,
        headline = UiText.Resource(Res.string.scenario_index_headline),
        stages = documentIndexScanStages(workspacePath) + documentIndexCommitStages(workspacePath),
    )

private fun documentIndexScanStages(workspacePath: String): List<StreamStageSpec> =
    listOf(
        StreamStageSpec.ThinkStage(
            id = "index-think-1",
            thought = "需要为工作区 $workspacePath 构建离线全文索引。先对比增量文件时间戳跳过未变更文档，再按标题层级切分语义段落并批量提交至本地 SQLite FTS5 表。",
            durationMillis = INDEX_THOUGHT_1_MS,
        ),
        StreamStageSpec.NarrationStage(
            id = "index-intro",
            durationMillis = INDEX_INTRO_MS,
            textBuilder = { "正在检查工作区 `$workspacePath` 的文档变更状态。" },
        ),
        StreamStageSpec.ToolStage(
            id = "index-diff",
            toolName = "workspace.diff",
            command = "git status --porcelain $workspacePath",
            totalChunks = INDEX_DIFF_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            runningSummary = { _, _ -> UiText.Resource(Res.string.scenario_index_diff_running) },
            completedSummary = uiResource(Res.string.scenario_index_diff_done, workspacePath),
            logLineAtChunk = { chunk, total -> indexLogLine("index-diff", workspacePath, chunk, total) },
        ),
        StreamStageSpec.NarrationStage(
            id = "index-after-diff",
            durationMillis = INDEX_AFTER_DIFF_MS,
            textBuilder = { "共识别出 **64 份新增或修改文档**。接下来提取正文并按 `64 token` 重叠窗口切分段落块：" },
        ),
        StreamStageSpec.ToolStage(
            id = "index-chunk",
            toolName = "doc.chunk",
            command = "chunk-markdown --overlap=64 $workspacePath",
            totalChunks = INDEX_PARSE_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = INDEX_DOC_COUNT,
            runningSummary = { done, total -> uiResource(Res.string.scenario_index_chunk_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_index_chunk_done),
            logLineAtChunk = { chunk, total -> indexLogLine("index-chunk", workspacePath, chunk, total) },
        ),
    )

private fun documentIndexCommitStages(workspacePath: String): List<StreamStageSpec> =
    listOf(
        StreamStageSpec.ThinkStage(
            id = "index-think-2",
            thought = "64 份文档切分为 920 个段落块。开启 WAL 模式按事务页批量写入 SQLite FTS5，控制单事务体积防止阻塞前台读取。",
            durationMillis = INDEX_THOUGHT_2_MS,
        ),
        StreamStageSpec.ToolStage(
            id = "index-commit",
            toolName = "sqlite.fts5",
            command = "sqlite3 index.db 'INSERT INTO docs_fts ...'",
            totalChunks = INDEX_COMMIT_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = INDEX_PAGE_BATCHES,
            thermalNoteAtChunk = INDEX_THERMAL_CHUNK,
            runningSummary = { done, total -> uiResource(Res.string.scenario_index_commit_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_index_commit_done),
            logLineAtChunk = { chunk, total -> indexLogLine("index-commit", workspacePath, chunk, total) },
        ),
        StreamStageSpec.NarrationStage(
            id = "index-summary",
            durationMillis = INDEX_SUMMARY_MS,
            totalUnits = SUMMARY_TEXT_SLICES,
            isFinalAnswer = true,
            textBuilder = ::formatDocumentIndexReport,
        ),
    )

private fun indexLogLine(
    stageId: String,
    workspacePath: String,
    chunk: Int,
    total: Int,
): String? =
    when (stageId) {
        "index-diff" -> {
            when (chunk) {
                LOG_CHUNK_FOUR -> "scanning $workspacePath/**/*.md, *.pdf, *.txt"
                total -> "delta ready: 64 modified/unindexed files, 118 unchanged skipped"
                else -> null
            }
        }

        "index-chunk" -> {
            if (chunk == 1 || chunk % LOG_STRIDE_SEVEN == 0 || chunk == total) {
                val docs = (chunk * INDEX_DOC_COUNT) / total
                val passages = (chunk * PASSAGES_TOTAL) / total
                "[chunker] parsed $docs/$INDEX_DOC_COUNT files -> $passages passages"
            } else {
                null
            }
        }

        "index-commit" -> {
            if (chunk == 1 || chunk % LOG_STRIDE_EIGHT == 0 || chunk == total) {
                val pages = (chunk * INDEX_PAGE_BATCHES) / total
                "[fts5-wal] committed page batch $pages/$INDEX_PAGE_BATCHES (p95 query=3.8ms)"
            } else {
                null
            }
        }

        else -> {
            null
        }
    }

private fun formatDocumentIndexReport(context: TaskRunContext): String {
    val seconds = (context.elapsedMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    val attached =
        if (context.attachmentNames.isEmpty()) {
            "无额外附件"
        } else {
            context.attachmentNames.joinToString(", ")
        }
    return buildString {
        append("### 本地离线全文索引构建完成\n\n")
        append("- **绑定工作区**：`")
        append(context.workspacePath)
        append("`\n- **合并附件输入**：")
        append(attached)
        append("\n- **构建总耗时**：")
        append(seconds)
        append("s（共写入 920 个语义段落块，本地检索平均延迟 `3.8ms`）\n\n")
        append("离线模式或飞行模式下均可直接毫秒级检索当前工作区的全部笔记与文档。")
    }
}

internal fun buildGenericWorkspaceScenario(workspacePath: String): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.GenericWorkspace,
        headline = UiText.Resource(Res.string.scenario_generic_headline),
        stages =
            listOf(
                StreamStageSpec.ThinkStage(
                    id = "generic-think-1",
                    thought = "正在分析任务意图与约束边界。先扫描工作区 $workspacePath 收集关联上下文文件，再逐项执行顺序一致性核查，最后整理可落地的执行报告。",
                    durationMillis = GENERIC_THOUGHT_1_MS,
                ),
                StreamStageSpec.NarrationStage(
                    id = "generic-intro",
                    durationMillis = GENERIC_INTRO_MS,
                    textBuilder = { "我先扫描工作区 `$workspacePath` 下的相关文件与结构上下文。" },
                ),
                StreamStageSpec.ToolStage(
                    id = "generic-scan",
                    toolName = "workspace.scan",
                    command = "rg --files $workspacePath",
                    totalChunks = GENERIC_SCAN_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = GENERIC_CONTEXT_ITEMS,
                    runningSummary = { done, total -> uiResource(Res.string.scenario_generic_scan_running, done, total) },
                    completedSummary = uiResource(Res.string.scenario_generic_scan_done, workspacePath),
                    logLineAtChunk = { chunk, total -> genericLogLine("generic-scan", workspacePath, chunk, total) },
                ),
                StreamStageSpec.ThinkStage(
                    id = "generic-think-2",
                    thought = "已收集 42 个相关上下文文件。现在按单智能体顺序链路逐条核对依赖关系与边界条件，确保输出建议可直接执行。",
                    durationMillis = GENERIC_THOUGHT_2_MS,
                ),
                StreamStageSpec.ToolStage(
                    id = "generic-verify",
                    toolName = "agent.verify",
                    command = "verify-invariants --sequential $workspacePath",
                    totalChunks = GENERIC_VERIFY_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = GENERIC_CHECK_RULES,
                    runningSummary = { done, total -> uiResource(Res.string.scenario_generic_verify_running, done, total) },
                    completedSummary = UiText.Resource(Res.string.scenario_generic_verify_done),
                    logLineAtChunk = { chunk, total -> genericLogLine("generic-verify", workspacePath, chunk, total) },
                ),
                StreamStageSpec.NarrationStage(
                    id = "generic-summary",
                    durationMillis = GENERIC_SUMMARY_MS,
                    totalUnits = SUMMARY_TEXT_SLICES,
                    isFinalAnswer = true,
                    textBuilder = ::formatGenericWorkspaceReport,
                ),
            ),
    )

private fun genericLogLine(
    stageId: String,
    workspacePath: String,
    chunk: Int,
    total: Int,
): String? =
    when (stageId) {
        "generic-scan" -> {
            if (chunk == 1 || chunk % LOG_STRIDE_SEVEN == 0 || chunk == total) {
                val files = (chunk * GENERIC_CONTEXT_ITEMS) / total
                "[scan] indexed $files/$GENERIC_CONTEXT_ITEMS context files under $workspacePath"
            } else {
                null
            }
        }

        "generic-verify" -> {
            if (chunk == 1 || chunk % LOG_STRIDE_EIGHT == 0 || chunk == total) {
                val rules = (chunk * GENERIC_CHECK_RULES) / total
                "[verify] invariant check $rules/$GENERIC_CHECK_RULES -> PASS"
            } else {
                null
            }
        }

        else -> {
            null
        }
    }

private fun formatGenericWorkspaceReport(context: TaskRunContext): String {
    val seconds = (context.elapsedMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    val topic = context.prompt.ifBlank { "当前工作区任务" }
    val constraints =
        if (context.userConstraints.isEmpty()) {
            ""
        } else {
            "\n- **补充约束**：" + context.userConstraints.joinToString("；")
        }
    return buildString {
        append("### 单智能体执行总结\n\n")
        append("- **任务目标**：")
        append(topic)
        append("\n- **目标工作区**：`")
        append(context.workspacePath)
        append("`\n- **累计运行耗时**：")
        append(seconds)
        append("s")
        append(constraints)
        append("\n\n已完成工作区上下文扫描与 12 项顺序一致性检查，你可以基于当前结论继续追问或细化下一步指令。")
    }
}

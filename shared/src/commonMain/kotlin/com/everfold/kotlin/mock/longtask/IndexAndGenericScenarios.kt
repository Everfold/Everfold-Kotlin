package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.scenario_generic_headline
import everfold.shared.generated.resources.scenario_generic_obs_1
import everfold.shared.generated.resources.scenario_generic_obs_2
import everfold.shared.generated.resources.scenario_generic_obs_3
import everfold.shared.generated.resources.scenario_generic_step_1
import everfold.shared.generated.resources.scenario_generic_step_2
import everfold.shared.generated.resources.scenario_generic_step_3
import everfold.shared.generated.resources.scenario_generic_step_4
import everfold.shared.generated.resources.scenario_index_headline
import everfold.shared.generated.resources.scenario_index_obs_1
import everfold.shared.generated.resources.scenario_index_obs_2
import everfold.shared.generated.resources.scenario_index_obs_3
import everfold.shared.generated.resources.scenario_index_step_1
import everfold.shared.generated.resources.scenario_index_step_2
import everfold.shared.generated.resources.scenario_index_step_3
import everfold.shared.generated.resources.scenario_index_step_4

private const val CHUNK_DELAY_MS = 500L
private const val FINAL_STEP_DELAY_MS = 400L

private const val INDEX_DIFF_CHUNKS = 10
private const val INDEX_PARSE_CHUNKS = 36
private const val INDEX_DOC_COUNT = 64
private const val INDEX_COMMIT_CHUNKS = 36
private const val INDEX_PAGE_BATCHES = 48
private const val INDEX_THERMAL_CHUNK = 18
private const val INDEX_STREAM_MS = 9_500L

private const val GENERIC_PLAN_CHUNKS = 10
private const val GENERIC_SCAN_CHUNKS = 28
private const val GENERIC_CONTEXT_ITEMS = 42
private const val GENERIC_VERIFY_CHUNKS = 38
private const val GENERIC_CHECK_RULES = 12
private const val GENERIC_STREAM_MS = 10_500L
private const val MILLIS_PER_SEC = 1000L

internal fun buildDocumentIndexScenario(workspacePath: String): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.DocumentIndex,
        headline = UiText.Resource(Res.string.scenario_index_headline),
        streamDurationMillis = INDEX_STREAM_MS,
        steps =
            listOf(
                StepSpec(
                    id = "index-diff",
                    title = UiText.Resource(Res.string.scenario_index_step_1),
                    toolName = "workspace.diff",
                    commandPreview = UiText.Literal("git status --porcelain $workspacePath"),
                    totalChunks = INDEX_DIFF_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    observationBuilder = { _, _ -> uiResource(Res.string.scenario_index_obs_1, workspacePath) },
                ),
                StepSpec(
                    id = "index-chunk",
                    title = UiText.Resource(Res.string.scenario_index_step_2),
                    toolName = "doc.chunk",
                    commandPreview = UiText.Literal("chunk-markdown --overlap=64 $workspacePath"),
                    totalChunks = INDEX_PARSE_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = INDEX_DOC_COUNT,
                    observationBuilder = { done, total -> uiResource(Res.string.scenario_index_obs_2, done, total) },
                ),
                StepSpec(
                    id = "index-commit",
                    title = UiText.Resource(Res.string.scenario_index_step_3),
                    toolName = "sqlite.fts5",
                    commandPreview = UiText.Literal("sqlite3 index.db 'INSERT INTO docs_fts ...'"),
                    totalChunks = INDEX_COMMIT_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = INDEX_PAGE_BATCHES,
                    thermalNoteAtChunk = INDEX_THERMAL_CHUNK,
                    observationBuilder = { done, total -> uiResource(Res.string.scenario_index_obs_3, done, total) },
                ),
                StepSpec(
                    id = "index-report",
                    title = UiText.Resource(Res.string.scenario_index_step_4),
                    toolName = "report.stream",
                    commandPreview = UiText.Literal("fts-benchmark --warmup=3"),
                    totalChunks = 1,
                    chunkDelayMillis = FINAL_STEP_DELAY_MS,
                ),
            ),
        reportBuilder = ::formatDocumentIndexReport,
    )

private fun formatDocumentIndexReport(context: TaskRunContext): String {
    val seconds = (context.elapsedMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    val attached =
        if (context.attachmentNames.isEmpty()) {
            "无额外上传文件"
        } else {
            context.attachmentNames.joinToString(", ")
        }
    return buildString {
        append("### 本地离线全文索引已就绪\n\n")
        append("- **绑定工作区**：`")
        append(context.workspacePath)
        append("`\n")
        append("- **合并附件输入**：")
        append(attached)
        append("\n")
        append("- **构建总耗时**：")
        append(seconds)
        append("s（共写入 920 个语义段落块，平均本地检索延迟 `3.8ms`）\n\n")
        append("现在即使处于飞行模式或无网络环境，也可在当前设备直接检索全部笔记与归档章节。")
    }
}

internal fun buildGenericWorkspaceScenario(workspacePath: String): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.GenericWorkspace,
        headline = UiText.Resource(Res.string.scenario_generic_headline),
        streamDurationMillis = GENERIC_STREAM_MS,
        steps =
            listOf(
                StepSpec(
                    id = "generic-plan",
                    title = UiText.Resource(Res.string.scenario_generic_step_1),
                    toolName = "task.plan",
                    commandPreview = UiText.Literal("plan --single-agent --workspace=$workspacePath"),
                    totalChunks = GENERIC_PLAN_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    observationBuilder = { _, _ -> uiResource(Res.string.scenario_generic_obs_1, workspacePath) },
                ),
                StepSpec(
                    id = "generic-scan",
                    title = UiText.Resource(Res.string.scenario_generic_step_2),
                    toolName = "workspace.scan",
                    commandPreview = UiText.Literal("rg --files $workspacePath"),
                    totalChunks = GENERIC_SCAN_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = GENERIC_CONTEXT_ITEMS,
                    observationBuilder = { done, total -> uiResource(Res.string.scenario_generic_obs_2, done, total) },
                ),
                StepSpec(
                    id = "generic-verify",
                    title = UiText.Resource(Res.string.scenario_generic_step_3),
                    toolName = "agent.verify",
                    commandPreview = UiText.Literal("verify-invariants --sequential"),
                    totalChunks = GENERIC_VERIFY_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = GENERIC_CHECK_RULES,
                    observationBuilder = { done, total -> uiResource(Res.string.scenario_generic_obs_3, done, total) },
                ),
                StepSpec(
                    id = "generic-report",
                    title = UiText.Resource(Res.string.scenario_generic_step_4),
                    toolName = "report.stream",
                    commandPreview = UiText.Literal("emit-summary --markdown"),
                    totalChunks = 1,
                    chunkDelayMillis = FINAL_STEP_DELAY_MS,
                ),
            ),
        reportBuilder = ::formatGenericWorkspaceReport,
    )

private fun formatGenericWorkspaceReport(context: TaskRunContext): String {
    val seconds = (context.elapsedMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    val topic = context.prompt.ifBlank { "当前工作区任务" }
    val constraints =
        if (context.userConstraints.isEmpty()) {
            ""
        } else {
            "\n- **追加约束**：" + context.userConstraints.joinToString("；")
        }
    return buildString {
        append("### 单智能体本机执行总结\n\n")
        append("- **任务目标**：")
        append(topic)
        append("\n- **目标工作区**：`")
        append(context.workspacePath)
        append("`\n- **累计执行耗时**：")
        append(seconds)
        append("s")
        append(constraints)
        append("\n\n已完成工作区上下文检索与顺序一致性核查。全部中间检查点均已保存，你可以继续基于此结果细化下一步指令。")
    }
}

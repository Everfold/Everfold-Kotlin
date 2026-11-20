package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.GateOption
import com.everfold.kotlin.feature.conversation.WaitingGate
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.scenario_diag_headline
import everfold.shared.generated.resources.scenario_diag_obs_1
import everfold.shared.generated.resources.scenario_diag_obs_2
import everfold.shared.generated.resources.scenario_diag_obs_3
import everfold.shared.generated.resources.scenario_diag_obs_4
import everfold.shared.generated.resources.scenario_diag_step_1
import everfold.shared.generated.resources.scenario_diag_step_2
import everfold.shared.generated.resources.scenario_diag_step_3
import everfold.shared.generated.resources.scenario_diag_step_4
import everfold.shared.generated.resources.scenario_diag_step_5
import everfold.shared.generated.resources.scenario_storage_gate_prompt
import everfold.shared.generated.resources.scenario_storage_gate_risk
import everfold.shared.generated.resources.scenario_storage_headline
import everfold.shared.generated.resources.scenario_storage_obs_1
import everfold.shared.generated.resources.scenario_storage_obs_2
import everfold.shared.generated.resources.scenario_storage_obs_3
import everfold.shared.generated.resources.scenario_storage_obs_5
import everfold.shared.generated.resources.scenario_storage_opt_archive_only
import everfold.shared.generated.resources.scenario_storage_opt_clean_all
import everfold.shared.generated.resources.scenario_storage_step_1
import everfold.shared.generated.resources.scenario_storage_step_2
import everfold.shared.generated.resources.scenario_storage_step_3
import everfold.shared.generated.resources.scenario_storage_step_4
import everfold.shared.generated.resources.scenario_storage_step_5
import everfold.shared.generated.resources.scenario_storage_step_6

private const val CHUNK_DELAY_MS = 500L
private const val GATE_ACK_DELAY_MS = 400L
private const val STORAGE_STAT_CHUNKS = 8
private const val STORAGE_SCAN_CHUNKS = 30
private const val STORAGE_SCAN_FILES = 1420
private const val STORAGE_HASH_CHUNKS = 28
private const val STORAGE_GATE_CHUNKS = 2
private const val STORAGE_MOVE_CHUNKS = 30
private const val STORAGE_STREAM_MS = 7_500L

private const val DIAG_VITALS_CHUNKS = 10
private const val DIAG_LOG_CHUNKS = 28
private const val DIAG_LOG_SEGMENTS = 84
private const val DIAG_TRACE_CHUNKS = 36
private const val DIAG_TRACE_SAMPLES = 360
private const val DIAG_FAULT_AT_CHUNK = 20
private const val DIAG_POWER_CHUNKS = 30
private const val DIAG_WAKE_SOURCES = 18
private const val DIAG_STREAM_MS = 10_000L
private const val MILLIS_PER_SEC = 1000L

internal const val GATE_OPT_CLEAN_ALL = "clean_all"
internal const val GATE_OPT_ARCHIVE_ONLY = "archive_only"

internal fun buildStorageArchiveScenario(): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.StorageArchive,
        headline = UiText.Resource(Res.string.scenario_storage_headline),
        streamDurationMillis = STORAGE_STREAM_MS,
        steps = storageArchiveSteps(),
        reportBuilder = ::formatStorageArchiveReport,
    )

private fun storageArchiveSteps(): List<StepSpec> =
    listOf(
        StepSpec(
            id = "storage-stat",
            title = UiText.Resource(Res.string.scenario_storage_step_1),
            toolName = "storage.stat",
            commandPreview = UiText.Literal("stat -f /storage/emulated/0/{Download,Pictures/Screenshots}"),
            totalChunks = STORAGE_STAT_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            observationBuilder = { _, _ -> UiText.Resource(Res.string.scenario_storage_obs_1) },
        ),
        StepSpec(
            id = "storage-walk",
            title = UiText.Resource(Res.string.scenario_storage_step_2),
            toolName = "fs.walk",
            commandPreview = UiText.Literal("walk --batch=50 /storage/emulated/0/Download /Pictures/Screenshots"),
            totalChunks = STORAGE_SCAN_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = STORAGE_SCAN_FILES,
            observationBuilder = { done, total -> uiResource(Res.string.scenario_storage_obs_2, done, total) },
        ),
        StepSpec(
            id = "storage-hash",
            title = UiText.Resource(Res.string.scenario_storage_step_3),
            toolName = "hash.sha256",
            commandPreview = UiText.Literal("sha256sum --dedup *.apk *.zip *.pdf"),
            totalChunks = STORAGE_HASH_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = STORAGE_HASH_CHUNKS,
            observationBuilder = { done, total -> uiResource(Res.string.scenario_storage_obs_3, done, total) },
        ),
        StepSpec(
            id = "storage-confirm",
            title = UiText.Resource(Res.string.scenario_storage_step_4),
            toolName = "policy.confirm",
            commandPreview = UiText.Literal("confirm --reclaim=612MB --duplicates=14"),
            totalChunks = STORAGE_GATE_CHUNKS,
            chunkDelayMillis = GATE_ACK_DELAY_MS,
            gate = storageCleanupGate(),
        ),
        StepSpec(
            id = "storage-move",
            title = UiText.Resource(Res.string.scenario_storage_step_5),
            toolName = "fs.batchMove",
            commandPreview = UiText.Literal("mv --journal --verify /storage/emulated/0/Archive/2026-11"),
            totalChunks = STORAGE_MOVE_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = STORAGE_MOVE_CHUNKS,
            observationBuilder = { done, total -> uiResource(Res.string.scenario_storage_obs_5, done, total) },
        ),
        StepSpec(
            id = "storage-report",
            title = UiText.Resource(Res.string.scenario_storage_step_6),
            toolName = "report.stream",
            commandPreview = UiText.Literal("render --manifest /storage/emulated/0/Archive/2026-11/manifest.json"),
            totalChunks = 1,
            chunkDelayMillis = GATE_ACK_DELAY_MS,
        ),
    )

private fun storageCleanupGate(): WaitingGate =
    WaitingGate(
        id = "storage-cleanup-gate",
        prompt = UiText.Resource(Res.string.scenario_storage_gate_prompt),
        riskHint = UiText.Resource(Res.string.scenario_storage_gate_risk),
        options =
            listOf(
                GateOption(GATE_OPT_CLEAN_ALL, UiText.Resource(Res.string.scenario_storage_opt_clean_all)),
                GateOption(GATE_OPT_ARCHIVE_ONLY, UiText.Resource(Res.string.scenario_storage_opt_archive_only)),
            ),
    )

private fun formatStorageArchiveReport(context: TaskRunContext): String {
    val archiveOnly = context.selectedGateOptionId == GATE_OPT_ARCHIVE_ONLY
    val actionSummary =
        if (archiveOnly) {
            "仅执行按月归档（保留全部 14 个重复 APK 副包未删除）"
        } else {
            "已完成按月归档并清理 14 个哈希一致的冗余 APK（释放 **612 MB** 存储空间）"
        }
    val constraintsNote =
        if (context.userConstraints.isEmpty()) {
            ""
        } else {
            "\n\n**已应用的补充约束**：" + context.userConstraints.joinToString("；")
        }
    val seconds = (context.elapsedMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    return buildString {
        append("### 本机存储深度归档完成\n\n")
        append("经过 **")
        append(seconds)
        append("s** 的单智能体顺序扫描与哈希核验，")
        append(actionSummary)
        append("。")
        append(constraintsNote)
        append("\n\n| 目录分类 | 处理项数 | 变更结果 |\n|---|---|---|\n")
        append("| `Download/Packages` | 38 项 | ")
        append(if (archiveOnly) "移入 `Archive/2026-11/apk`（未删除）" else "保留最新构建，清理 14 个重复包 (-612 MB)")
        append(" |\n")
        append("| `Pictures/Screenshots` | 86 项 | 按月归档至 `Archive/2026-10` 与 `2026-11` |\n")
        append("| `Download/Documents` | 124 项 | 按扩展名建立离线目录清单 |\n\n")
        append("回滚校验清单已写入：`/storage/emulated/0/Archive/2026-11/rollback-manifest.json`。")
    }
}

internal fun buildDiagnosticsScenario(): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.Diagnostics,
        headline = UiText.Resource(Res.string.scenario_diag_headline),
        streamDurationMillis = DIAG_STREAM_MS,
        steps =
            listOf(
                StepSpec(
                    id = "diag-vitals",
                    title = UiText.Resource(Res.string.scenario_diag_step_1),
                    toolName = "device.vitals",
                    commandPreview = UiText.Literal("dumpsys battery && am get-standby-bucket"),
                    totalChunks = DIAG_VITALS_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    observationBuilder = { _, _ -> UiText.Resource(Res.string.scenario_diag_obs_1) },
                ),
                StepSpec(
                    id = "diag-logs",
                    title = UiText.Resource(Res.string.scenario_diag_step_2),
                    toolName = "log.index",
                    commandPreview = UiText.Literal("logcat -d -b main,system,crash --window=72h"),
                    totalChunks = DIAG_LOG_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = DIAG_LOG_SEGMENTS,
                    observationBuilder = { done, total -> uiResource(Res.string.scenario_diag_obs_2, done, total) },
                ),
                StepSpec(
                    id = "diag-trace",
                    title = UiText.Resource(Res.string.scenario_diag_step_3),
                    toolName = "trace.analyze",
                    commandPreview = UiText.Literal("trace-correlate --anr --lock-contention"),
                    totalChunks = DIAG_TRACE_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = DIAG_TRACE_SAMPLES,
                    autoFaultAtChunk = DIAG_FAULT_AT_CHUNK,
                    observationBuilder = { done, total -> uiResource(Res.string.scenario_diag_obs_3, done, total) },
                ),
                StepSpec(
                    id = "diag-power",
                    title = UiText.Resource(Res.string.scenario_diag_step_4),
                    toolName = "power.audit",
                    commandPreview = UiText.Literal("dumpsys batterystats --wakelock-audit"),
                    totalChunks = DIAG_POWER_CHUNKS,
                    chunkDelayMillis = CHUNK_DELAY_MS,
                    totalItems = DIAG_WAKE_SOURCES,
                    observationBuilder = { done, total -> uiResource(Res.string.scenario_diag_obs_4, done, total) },
                ),
                StepSpec(
                    id = "diag-report",
                    title = UiText.Resource(Res.string.scenario_diag_step_5),
                    toolName = "report.stream",
                    commandPreview = UiText.Literal("synthesize --root-cause --patch-preview"),
                    totalChunks = 1,
                    chunkDelayMillis = GATE_ACK_DELAY_MS,
                ),
            ),
        reportBuilder = ::formatDiagnosticsReport,
    )

private fun formatDiagnosticsReport(context: TaskRunContext): String {
    val seconds = (context.elapsedMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    val extra =
        if (context.userConstraints.isEmpty()) {
            ""
        } else {
            "\n\n> 补充过滤条件：" + context.userConstraints.joinToString("；")
        }
    return buildString {
        append("### 本机后台耗电与阻塞诊断报告（耗时 ")
        append(seconds)
        append("s）")
        append(extra)
        append("\n\n1. **主线程 I/O 阻塞热点**：在最近 72 小时日志分片中捕获 3 次超过 `420ms` 的同步偏好落盘阻塞。\n")
        append("2. **高频唤醒源**：离线同步失败后缺少指数退避，在弱网切后台期间产生 47 次短周期唤醒。\n\n")
        append("#### 建议修复（协程合并写盘与退避重试）\n\n")
        append("```kotlin\n")
        append("val writeChannel = Channel<Snapshot>(Channel.CONFLATED)\n")
        append("scope.launch(Dispatchers.IO) {\n")
        append("    for (snapshot in writeChannel) {\n")
        append("        atomicFile.writeBuffered(snapshot)\n")
        append("    }\n")
        append("}\n")
        append("```\n")
    }
}

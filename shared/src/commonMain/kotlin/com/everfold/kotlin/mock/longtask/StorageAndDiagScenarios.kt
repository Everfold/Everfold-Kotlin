package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.GateOption
import com.everfold.kotlin.feature.conversation.WaitingGate
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.scenario_storage_gate_prompt
import everfold.shared.generated.resources.scenario_storage_gate_risk
import everfold.shared.generated.resources.scenario_storage_hash_done
import everfold.shared.generated.resources.scenario_storage_hash_running
import everfold.shared.generated.resources.scenario_storage_headline
import everfold.shared.generated.resources.scenario_storage_move_done
import everfold.shared.generated.resources.scenario_storage_move_running
import everfold.shared.generated.resources.scenario_storage_opt_archive_only
import everfold.shared.generated.resources.scenario_storage_opt_clean_all
import everfold.shared.generated.resources.scenario_storage_stat_done
import everfold.shared.generated.resources.scenario_storage_stat_running
import everfold.shared.generated.resources.scenario_storage_walk_done
import everfold.shared.generated.resources.scenario_storage_walk_running

private const val CHUNK_DELAY_MS = 500L
private const val SHORT_THOUGHT_MS = 2_500L
private const val SHORT_NARRATION_MS = 1_500L
private const val MID_NARRATION_MS = 2_000L
private const val STORAGE_SUMMARY_MS = 5_500L
private const val MILLIS_PER_SEC = 1_000L

private const val STORAGE_STAT_CHUNKS = 7
private const val STORAGE_WALK_CHUNKS = 26
internal const val STORAGE_SCAN_FILES = 1_420
internal const val STORAGE_HASH_CHUNKS = 24
private const val STORAGE_MOVE_CHUNKS = 24

private const val LOG_STRIDE_FIVE = 5
private const val LOG_CHUNK_FIVE = 5
private const val LOG_CHUNK_SIX = 6
private const val LOG_CHUNK_TEN = 10
private const val LOG_CHUNK_TWELVE = 12
private const val LOG_CHUNK_EIGHTEEN = 18

internal const val GATE_OPT_CLEAN_ALL = "clean_all"
internal const val GATE_OPT_ARCHIVE_ONLY = "archive_only"

private val StorageStatLogs =
    mapOf(
        2 to "Filesystem      Size   Used  Avail  Use%  Mounted on",
        LOG_CHUNK_FIVE to "/dev/block/dm-5 256G  237.6G  18.4G  92.8% /storage/emulated/0",
        STORAGE_STAT_CHUNKS to "Scoped Storage access: Download(RW), Pictures/Screenshots(RW)",
    )

private val StorageHashLogs =
    mapOf(
        1 to "computing sha256 digests across 38 installer packages...",
        LOG_CHUNK_SIX to "8f4e2b9c...  everfold-preview-arm64 (1).apk  [DUPLICATE +44.2 MB]",
        LOG_CHUNK_TWELVE to "3c91a0d4...  telegram-foss-universal (1).apk [DUPLICATE +68.5 MB]",
        LOG_CHUNK_EIGHTEEN to "a71d09ef...  kotlin-docs-offline (2).zip     [DUPLICATE +112.0 MB]",
        STORAGE_HASH_CHUNKS to "verified 24/24 batches -> 14 redundant packages (612 MB reclaimable)",
    )

private val StorageMoveLogs =
    mapOf(
        2 to "mkdir -p /storage/emulated/0/Archive/{2026-10,2026-11}",
        LOG_CHUNK_TEN to "archived 86 screenshots -> Archive/2026-10 & Archive/2026-11",
        LOG_CHUNK_EIGHTEEN to "processed 38 package entries according to confirmed policy",
        STORAGE_MOVE_CHUNKS to "wrote journal -> /storage/emulated/0/Archive/2026-11/rollback-manifest.json",
    )

internal fun buildStorageArchiveScenario(): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.StorageArchive,
        headline = UiText.Resource(Res.string.scenario_storage_headline),
        stages = storageDiscoveryStages() + storageExecutionStages(),
    )

private fun storageDiscoveryStages(): List<StreamStageSpec> =
    listOf(
        StreamStageSpec.ThinkStage(
            id = "storage-think-1",
            thought = "用户希望深度整理本机 Download 与截图目录并清理重复安装包。先检查共享存储卷容量和读写权限，再分批遍历目录统计文件分布，避免一次性加载过多文件元数据阻塞内存。",
            durationMillis = SHORT_THOUGHT_MS,
        ),
        StreamStageSpec.NarrationStage(
            id = "storage-intro",
            durationMillis = SHORT_NARRATION_MS,
            textBuilder = { "我先检查本机共享存储分区的剩余容量与目录访问权限。" },
        ),
        StreamStageSpec.ToolStage(
            id = "storage-stat",
            toolName = "storage.stat",
            command = "stat -f /storage/emulated/0/{Download,Pictures/Screenshots}",
            totalChunks = STORAGE_STAT_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            runningSummary = { _, _ -> UiText.Resource(Res.string.scenario_storage_stat_running) },
            completedSummary = UiText.Resource(Res.string.scenario_storage_stat_done),
            logLineAtChunk = { chunk, _ -> StorageStatLogs[chunk] },
        ),
        StreamStageSpec.NarrationStage(
            id = "storage-after-stat",
            durationMillis = MID_NARRATION_MS,
            textBuilder = { "存储分区使用率已达 **92.8%**（剩余 `18.4 GB`）。接下来分批遍历 `Download/` 与 `Pictures/Screenshots/` 目录统计文件类型与体积。" },
        ),
        StreamStageSpec.ToolStage(
            id = "storage-walk",
            toolName = "fs.walk",
            command = "walk --batch=55 /storage/emulated/0/Download /Pictures/Screenshots",
            totalChunks = STORAGE_WALK_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = STORAGE_SCAN_FILES,
            runningSummary = { done, total -> uiResource(Res.string.scenario_storage_walk_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_storage_walk_done),
            logLineAtChunk = ::storageWalkLogLine,
        ),
        StreamStageSpec.ThinkStage(
            id = "storage-think-2",
            thought = "扫描发现 38 个 APK 安装包（1.68 GB）与 86 张旧截图。部分安装包带有 (1) 后缀，不能仅凭文件名判定重复，需计算 SHA-256 指纹做字节级一致性核验。",
            durationMillis = SHORT_THOUGHT_MS,
        ),
    )

private fun storageWalkLogLine(
    chunk: Int,
    total: Int,
): String? =
    if (chunk == 1 || chunk % LOG_STRIDE_FIVE == 0 || chunk == total) {
        val scanned = (chunk * STORAGE_SCAN_FILES) / total
        "[batch $chunk/$total] indexed $scanned/$STORAGE_SCAN_FILES entries (apk, pdf, png)"
    } else {
        null
    }

private fun storageExecutionStages(): List<StreamStageSpec> =
    listOf(
        StreamStageSpec.ToolStage(
            id = "storage-hash",
            toolName = "hash.sha256",
            command = "sha256sum --dedup /storage/emulated/0/Download/*.{apk,zip}",
            totalChunks = STORAGE_HASH_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = STORAGE_HASH_CHUNKS,
            runningSummary = { done, total -> uiResource(Res.string.scenario_storage_hash_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_storage_hash_done),
            logLineAtChunk = { chunk, _ -> StorageHashLogs[chunk] },
        ),
        StreamStageSpec.NarrationStage(
            id = "storage-pre-gate",
            durationMillis = MID_NARRATION_MS,
            textBuilder = {
                "已完成全部候选包指纹比对，确认 **14 个字节完全一致的冗余 APK（可释放 612 MB）** 以及 **86 张超过 30 天的历史截图**。执行批量变更前，请确认处理方式："
            },
        ),
        StreamStageSpec.ApprovalStage(
            id = "storage-confirm",
            gate = storageCleanupGate(),
        ),
        StreamStageSpec.ToolStage(
            id = "storage-move",
            toolName = "fs.batchMove",
            command = "mv --journal --verify /storage/emulated/0/Archive/2026-11",
            totalChunks = STORAGE_MOVE_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = STORAGE_MOVE_CHUNKS,
            runningSummary = { done, total -> uiResource(Res.string.scenario_storage_move_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_storage_move_done),
            logLineAtChunk = { chunk, _ -> StorageMoveLogs[chunk] },
        ),
        StreamStageSpec.NarrationStage(
            id = "storage-summary",
            durationMillis = STORAGE_SUMMARY_MS,
            totalUnits = SUMMARY_TEXT_SLICES,
            isFinalAnswer = true,
            textBuilder = ::formatStorageArchiveReport,
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
            "已按月归档全部文件（保留全部 14 个重复 APK 副包未删除）"
        } else {
            "已完成按月归档并清理 14 个字节一致的冗余 APK（释放 **612 MB** 存储空间）"
        }
    val constraintsNote =
        if (context.userConstraints.isEmpty()) {
            ""
        } else {
            "\n\n**已应用的补充约束**：" + context.userConstraints.joinToString("；")
        }
    val seconds = (context.elapsedMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    return buildString {
        append("### 本机存储归档总结\n\n")
        append("经过 **")
        append(seconds)
        append("s** 的顺序扫描与 SHA-256 指纹核验，")
        append(actionSummary)
        append("。")
        append(constraintsNote)
        append("\n\n| 目录分类 | 处理项数 | 变更结果 |\n|---|---|---|\n")
        append("| `Download/Packages` | 38 项 | ")
        append(if (archiveOnly) "移入 `Archive/2026-11/apk`（未删除）" else "保留原版构建，清理 14 个重复包 (-612 MB)")
        append(" |\n")
        append("| `Pictures/Screenshots` | 86 项 | 按月归档至 `Archive/2026-10` 与 `2026-11` |\n")
        append("| `Download/Documents` | 124 项 | 按扩展名生成离线检索目录清单 |\n\n")
        append("回滚校验清单：`/storage/emulated/0/Archive/2026-11/rollback-manifest.json`。")
    }
}

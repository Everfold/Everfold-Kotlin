package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.scenario_diag_headline
import everfold.shared.generated.resources.scenario_diag_logs_done
import everfold.shared.generated.resources.scenario_diag_logs_running
import everfold.shared.generated.resources.scenario_diag_power_done
import everfold.shared.generated.resources.scenario_diag_power_running
import everfold.shared.generated.resources.scenario_diag_trace_done
import everfold.shared.generated.resources.scenario_diag_trace_running
import everfold.shared.generated.resources.scenario_diag_vitals_done
import everfold.shared.generated.resources.scenario_diag_vitals_running

private const val CHUNK_DELAY_MS = 500L
private const val SHORT_THOUGHT_MS = 2_500L
private const val SHORT_NARRATION_MS = 1_500L
private const val MID_NARRATION_MS = 2_000L
private const val DIAG_SUMMARY_MS = 6_500L
private const val MILLIS_PER_SEC = 1_000L

private const val DIAG_VITALS_CHUNKS = 9
private const val DIAG_LOG_CHUNKS = 26
private const val DIAG_LOG_SEGMENTS = 84
private const val DIAG_TRACE_CHUNKS = 32
private const val DIAG_TRACE_SAMPLES = 360
private const val DIAG_FAULT_AT_CHUNK = 16
private const val DIAG_POWER_CHUNKS = 22
private const val DIAG_WAKE_SOURCES = 18

private const val LOG_STRIDE_SIX = 6
private const val LOG_CHUNK_THREE = 3
private const val LOG_CHUNK_FOUR = 4
private const val LOG_CHUNK_FIVE = 5
private const val LOG_CHUNK_SIX = 6
private const val LOG_CHUNK_TWELVE = 12
private const val LOG_CHUNK_FOURTEEN = 14
private const val LOG_CHUNK_TWENTY_TWO = 22
private const val STALL_TOTAL = 3

private val DiagVitalsLogs =
    mapOf(
        LOG_CHUNK_THREE to "temperature: 384 (38.4°C) | level: 61% | status: DISCHARGING",
        LOG_CHUNK_SIX to "standby_bucket: ACTIVE (10) | bg_wakeups_24h: 47",
        DIAG_VITALS_CHUNKS to "doze_light_idle_exits: 19 | power_save_whitelist: false",
    )

private val DiagTraceLogs =
    mapOf(
        LOG_CHUNK_FOUR to "sample #45  main (BLOCKED) waiting on PrefsDiskLock",
        LOG_CHUNK_TWELVE to "sample #135 StrictModeDiskWriteViolation 438ms SyncPrefs.commit()",
        LOG_CHUNK_TWENTY_TWO to "sample #248 correlated 3 stalls to unbuffered sync preference writes",
        DIAG_TRACE_CHUNKS to "hotspot isolated: SyncPrefs.commit() blocking main looper",
    )

private val DiagPowerLogs =
    mapOf(
        LOG_CHUNK_FIVE to "PartialWakeLock *sync*/OfflineRetryLoop held 14m22s (47 acquisitions)",
        LOG_CHUNK_FOURTEEN to "retry cadence observed: fixed 15s interval without backoff",
        DIAG_POWER_CHUNKS to "audit complete: exponential backoff + conflated IO recommended",
    )

internal fun buildDiagnosticsScenario(): ScenarioSpec =
    ScenarioSpec(
        kind = ScenarioKind.Diagnostics,
        headline = UiText.Resource(Res.string.scenario_diag_headline),
        stages = diagnosticsCollectionStages() + diagnosticsCorrelationStages(),
    )

private fun diagnosticsCollectionStages(): List<StreamStageSpec> =
    listOf(
        StreamStageSpec.ThinkStage(
            id = "diag-think-1",
            thought = "排查近期后台异常耗电与卡顿，需要先采集电池温控和待机桶状态，再检索最近 72 小时沙箱日志分片定位 ANR 阻塞栈与高频唤醒锁。",
            durationMillis = SHORT_THOUGHT_MS,
        ),
        StreamStageSpec.NarrationStage(
            id = "diag-intro",
            durationMillis = SHORT_NARRATION_MS,
            textBuilder = { "我先读取当前设备的电池温控快照与应用待机桶状态。" },
        ),
        StreamStageSpec.ToolStage(
            id = "diag-vitals",
            toolName = "device.vitals",
            command = "dumpsys battery && am get-standby-bucket",
            totalChunks = DIAG_VITALS_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            runningSummary = { _, _ -> UiText.Resource(Res.string.scenario_diag_vitals_running) },
            completedSummary = UiText.Resource(Res.string.scenario_diag_vitals_done),
            logLineAtChunk = { chunk, _ -> DiagVitalsLogs[chunk] },
        ),
        StreamStageSpec.NarrationStage(
            id = "diag-after-vitals",
            durationMillis = MID_NARRATION_MS,
            textBuilder = { "过去 24 小时记录到 **47 次后台唤醒**，机身温度 `38.4°C`。下面拉取最近 72 小时的沙箱日志分片构建时间轴。" },
        ),
        StreamStageSpec.ToolStage(
            id = "diag-logs",
            toolName = "log.index",
            command = "logcat -d -b main,system,crash --window=72h",
            totalChunks = DIAG_LOG_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = DIAG_LOG_SEGMENTS,
            runningSummary = { done, total -> uiResource(Res.string.scenario_diag_logs_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_diag_logs_done),
            logLineAtChunk = ::diagLogSegmentLine,
        ),
    )

private fun diagLogSegmentLine(
    chunk: Int,
    total: Int,
): String? =
    if (chunk == 1 || chunk % LOG_STRIDE_SIX == 0 || chunk == total) {
        val seg = (chunk * DIAG_LOG_SEGMENTS) / total
        val stalls = (chunk * STALL_TOTAL) / total
        "[segment $seg/$DIAG_LOG_SEGMENTS] parsed logcat window; stalls >420ms: $stalls"
    } else {
        null
    }

private fun diagnosticsCorrelationStages(): List<StreamStageSpec> =
    listOf(
        StreamStageSpec.ThinkStage(
            id = "diag-think-2",
            thought = "日志中捕获到 3 次超过 420ms 的主线程卡顿，且时间点与后台离线同步重试高度重合。接下来关联调用栈样本并审计 WakeLock 持锁周期。",
            durationMillis = SHORT_THOUGHT_MS,
        ),
        StreamStageSpec.ToolStage(
            id = "diag-trace",
            toolName = "trace.analyze",
            command = "trace-correlate --anr --lock-contention",
            totalChunks = DIAG_TRACE_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = DIAG_TRACE_SAMPLES,
            autoFaultAtChunk = DIAG_FAULT_AT_CHUNK,
            runningSummary = { done, total -> uiResource(Res.string.scenario_diag_trace_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_diag_trace_done),
            logLineAtChunk = { chunk, _ -> DiagTraceLogs[chunk] },
        ),
        StreamStageSpec.NarrationStage(
            id = "diag-after-trace",
            durationMillis = MID_NARRATION_MS,
            textBuilder = { "调用栈确认主线程阻塞源于同步偏好写盘锁竞争。继续审计后台 `WakeLock` 唤醒频率：" },
        ),
        StreamStageSpec.ToolStage(
            id = "diag-power",
            toolName = "power.audit",
            command = "dumpsys batterystats --wakelock-audit",
            totalChunks = DIAG_POWER_CHUNKS,
            chunkDelayMillis = CHUNK_DELAY_MS,
            totalItems = DIAG_WAKE_SOURCES,
            runningSummary = { done, total -> uiResource(Res.string.scenario_diag_power_running, done, total) },
            completedSummary = UiText.Resource(Res.string.scenario_diag_power_done),
            logLineAtChunk = { chunk, _ -> DiagPowerLogs[chunk] },
        ),
        StreamStageSpec.NarrationStage(
            id = "diag-summary",
            durationMillis = DIAG_SUMMARY_MS,
            totalUnits = SUMMARY_TEXT_SLICES,
            isFinalAnswer = true,
            textBuilder = ::formatDiagnosticsReport,
        ),
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
        append("### 本机后台耗电与阻塞诊断结论（耗时 ")
        append(seconds)
        append("s）")
        append(extra)
        append("\n\n1. **主线程 I/O 阻塞热点**：最近 72 小时内捕获 3 次超过 `420ms` 的同步写盘锁竞争（`SyncPrefs.commit()`）。\n")
        append("2. **高频唤醒根因**：离线同步失败后采用固定 `15s` 重试，缺少指数退避，弱网待机期间触发 47 次 `PartialWakeLock`。\n\n")
        append("#### 建议修复示例\n\n")
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

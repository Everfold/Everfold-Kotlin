package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.DeviceNoteSeverity
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.longtask_note_resumed

private val StorageKeywords =
    listOf("download", "screenshot", "apk", "storage", "archive", "clean", "整理", "下载", "截图", "清理", "存储", "归档", "重复")
private val DiagnosticsKeywords =
    listOf("log", "crash", "anr", "battery", "drain", "diagnos", "power", "日志", "崩溃", "耗电", "诊断", "卡顿", "发热", "省电")
private val IndexKeywords =
    listOf("index", "search", "offline", "fts", "document", "索引", "离线", "检索", "知识库", "文档")

private const val PRESEEDED_STEP_INDEX = 2
private const val PRESEEDED_CHUNK_OFFSET = 15
private const val PRESEEDED_STEP_0_MS = 4_000L
private const val PRESEEDED_STEP_1_MS = 15_000L
private const val PRESEEDED_ELAPSED_MS = 26_500L
private const val PRESEEDED_PERCENT = 43

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
        return TaskRunRecord(
            taskId = "task-preseeded-storage",
            conversationId = PAUSED_SAMPLE_CONVERSATION_ID,
            connectionId = BUILT_IN_CONNECTION_ID,
            assistantMessageId = PAUSED_SAMPLE_ASSISTANT_MESSAGE_ID,
            prompt = "深度整理本机 Download 与截图目录并清理重复安装包",
            workspacePath = "/workspace",
            attachmentNames = emptyList(),
            scenario = scenario,
            currentStepIndex = PRESEEDED_STEP_INDEX,
            currentChunkOffset = PRESEEDED_CHUNK_OFFSET,
            stepDurations = mapOf(0 to PRESEEDED_STEP_0_MS, 1 to PRESEEDED_STEP_1_MS),
            elapsedMillis = PRESEEDED_ELAPSED_MS,
            deviceNote =
                DeviceEnvironmentNote(
                    severity = DeviceNoteSeverity.Info,
                    message =
                        uiResource(
                            Res.string.longtask_note_resumed,
                            PRESEEDED_STEP_INDEX + 1,
                            scenario.steps.size,
                            PRESEEDED_PERCENT,
                        ),
                ),
        )
    }
}

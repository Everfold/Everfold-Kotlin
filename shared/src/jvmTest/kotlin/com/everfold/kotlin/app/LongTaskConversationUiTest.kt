package com.everfold.kotlin.app

import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertTrue

private const val NARROW_WIDTH = 390
private const val WIDE_WIDTH = 1280
private const val EXPANDED_BREAKPOINT = 900
private const val MAX_STATUS_SUMMARY_HEIGHT = 28f

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class LongTaskConversationUiTest(
    private val width: Int,
) {
    @Test
    fun preseededPausedStorageTaskResumesHitsWaitingGateAndCompletesReport() =
        workspaceUiTest(width = width, enableLongTasks = true) { fixture ->
            openSessionFromSidebar("本机存储与下载目录整理")
            onNodeWithText("fs.walk", substring = true).assertDoesNotExist()
            onNodeWithText("hash.sha256", substring = true).assertIsDisplayed()
            val summaryBounds = onNodeWithContentDescription("执行详情").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(summaryBounds.height <= MAX_STATUS_SUMMARY_HEIGHT, "Status summary stays compact: ${summaryBounds.height}")

            onNodeWithText("继续执行").assertIsDisplayed().performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            waitForIdle()

            onNodeWithText("检测到 14 个重复历史 APK", substring = true).assertIsDisplayed()
            onNodeWithText("仅按月归档，不删除文件").assertIsDisplayed().performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            waitForIdle()

            onNodeWithText("本机存储深度归档完成", substring = true).assertIsDisplayed()
            onNodeWithText("已完成 · 共 6 步", substring = true).assertIsDisplayed()
            onNodeWithContentDescription("执行详情").performScrollTo().performClick()
            onNodeWithText("1. 探测挂载卷容量与分区权限").performScrollTo().assertIsDisplayed()
        }

    @Test
    fun welcomeLongTaskSuggestionRunsDiagnosticsRecoversFromFaultAndSupportsMenuThrottle() =
        workspaceUiTest(width = width, enableLongTasks = true) { fixture ->
            onNodeWithText("诊断本机近期后台耗电异常与崩溃日志").assertIsDisplayed().performClick()
            onNodeWithContentDescription("发送").performClick()

            runOnIdle {
                fixture.scheduler.advanceTimeBy(6_000L)
                fixture.scheduler.runCurrent()
            }
            waitForIdle()
            onNodeWithContentDescription("会话菜单").performClick()
            onNodeWithText("演示执行状态").performClick()
            onNodeWithText("模拟省电模式节流").assertIsDisplayed().performClick()
            onNodeWithText("系统省电模式已激活", substring = true).assertIsDisplayed()

            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            waitForIdle()
            onNodeWithText("设备省电限制导致文件分片锁超时", substring = true).assertIsDisplayed()
            onNodeWithText("重试").assertIsDisplayed().performClick()

            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            waitForIdle()
            onNodeWithText("本机后台耗电与阻塞诊断报告", substring = true).assertIsDisplayed()
        }

    private fun DesktopComposeUiTest.openSessionFromSidebar(title: String) {
        if (width < EXPANDED_BREAKPOINT) {
            onNodeWithContentDescription("打开工作台").performClick()
        }
        onNode(hasText(title) and hasClickAction()).performClick()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(NARROW_WIDTH), arrayOf(WIDE_WIDTH))
    }
}

package com.everfold.kotlin.app

import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test

private const val NARROW_WIDTH = 390
private const val WIDE_WIDTH = 1280
private const val EXPANDED_BREAKPOINT = 900

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class LongTaskConversationUiTest(
    private val width: Int,
) {
    @Test
    fun preseededPausedStorageTaskAutoCollapsesProcessOnCompletionAndExpandsOnDemand() =
        workspaceUiTest(width = width, enableLongTasks = true) { fixture ->
            openSessionFromSidebar("本机存储与下载目录整理")
            onNodeWithText("fs.batchMove", substring = true).assertDoesNotExist()
            onNodeWithText("fs.walk", substring = true).assertIsDisplayed()
            onNodeWithText("$ walk --batch=55", substring = true).assertDoesNotExist()
            onNodeWithText("hash.sha256", substring = true).assertIsDisplayed()
            onNodeWithText("$ sha256sum --dedup", substring = true).assertIsDisplayed()

            onNodeWithText("继续执行").assertIsDisplayed().performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            waitForIdle()

            onNodeWithText("请选择重复安装包与旧截图的处理方式", substring = true).assertIsDisplayed()
            onNodeWithText("仅按月归档，不删除文件").assertIsDisplayed().performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            waitForIdle()

            onNodeWithText("本机存储归档总结", substring = true).assertIsDisplayed()
            onNodeWithText("已折叠中间过程 · 4 次工具调用").assertIsDisplayed()
            onNodeWithText("fs.batchMove", substring = true).assertDoesNotExist()

            onNodeWithContentDescription("切换执行过程详情").performClick()
            onNodeWithText("fs.batchMove", substring = true).assertIsDisplayed().performClick()
            onNodeWithText("$ mv --journal --verify", substring = true).assertIsDisplayed()
        }

    @Test
    fun welcomeDiagnosticsSuggestionStreamsInterleavedFlowHandlesThrottleAndRetry() =
        workspaceUiTest(width = width, enableLongTasks = true) { fixture ->
            onNodeWithText("诊断本机近期后台耗电异常与崩溃日志").assertIsDisplayed().performClick()
            onNodeWithContentDescription("发送").performClick()

            runOnIdle {
                fixture.scheduler.advanceTimeBy(1_000L)
                fixture.scheduler.runCurrent()
            }
            waitForIdle()
            onNodeWithText("思考中…").assertIsDisplayed()
            onNodeWithText("power.audit", substring = true).assertDoesNotExist()

            runOnIdle {
                fixture.scheduler.advanceTimeBy(5_000L)
                fixture.scheduler.runCurrent()
            }
            waitForIdle()
            onNode(hasText("device.vitals", substring = true) and hasAnyAncestor(hasScrollAction())).assertIsDisplayed()
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
            onNodeWithText("本机后台耗电与阻塞诊断结论", substring = true).assertIsDisplayed()
            onNodeWithText("device.vitals", substring = true).assertDoesNotExist()
        }

    @Test
    fun processVisibilitySettingsControlRunningAndCompletedDefaultExpansion() =
        workspaceUiTest(width = width, enableLongTasks = true) { fixture ->
            openConversationSettings()
            onNodeWithText("运行中默认展开执行过程").performScrollTo().assertIsOn().performClick()
            onNodeWithText("运行中默认展开执行过程").assertIsOff()
            onNodeWithText("结束后默认展开执行过程").performScrollTo().assertIsOff().performClick()
            onNodeWithText("结束后默认展开执行过程").assertIsOn()
            returnToConversation()

            onNodeWithText("为当前工作区文档构建离线全文检索索引").assertIsDisplayed().performClick()
            onNodeWithContentDescription("发送").performClick()

            runOnIdle {
                fixture.scheduler.advanceTimeBy(6_000L)
                fixture.scheduler.runCurrent()
            }
            waitForIdle()
            onNodeWithContentDescription("切换执行过程详情").assertIsDisplayed()
            onNodeWithText("$ git status --porcelain", substring = true).assertDoesNotExist()

            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            waitForIdle()
            onNodeWithText("本地离线全文索引构建完成", substring = true).assertIsDisplayed()
            onNodeWithText("sqlite.fts5", substring = true).assertIsDisplayed()
        }

    @Test
    fun sessionTraceViewShowsEmptyStateAndInteractiveLoopWaterfall() =
        workspaceUiTest(width = width, enableLongTasks = true) {
            onNodeWithContentDescription("执行轨迹").assertIsDisplayed().performClick()
            onNodeWithText("当前会话暂无执行轨迹").assertIsDisplayed()
            onNodeWithContentDescription("返回会话").performClick()

            openSessionFromSidebar("本机存储与下载目录整理")
            onNodeWithContentDescription("执行轨迹").assertIsDisplayed().performClick()
            onNodeWithText("Loop #1").assertIsDisplayed()
            onNodeWithText("storage.stat").performScrollTo().assertIsDisplayed()
            onNodeWithText("fs.walk").performScrollTo().assertIsDisplayed().performClick()
            onNodeWithText("发出", substring = true).performScrollTo().assertIsDisplayed()
            onNodeWithText("返回", substring = true).performScrollTo().assertIsDisplayed()
            onNodeWithText("hash.sha256").performScrollTo().assertIsDisplayed()

            onNodeWithContentDescription("返回会话").performClick()
            onNodeWithText("继续执行").assertIsDisplayed()
        }

    private fun DesktopComposeUiTest.openSessionFromSidebar(title: String) {
        if (width < EXPANDED_BREAKPOINT) {
            onNodeWithContentDescription("打开工作台").performClick()
        }
        onNode(hasText(title) and hasClickAction()).performClick()
    }

    private fun DesktopComposeUiTest.openConversationSettings() {
        if (width < EXPANDED_BREAKPOINT) {
            onNodeWithContentDescription("打开工作台").performClick()
        }
        onNode(hasText("设置") and hasClickAction()).performClick()
        onNode(hasText("会话") and hasClickAction()).performClick()
    }

    private fun DesktopComposeUiTest.returnToConversation() {
        if (width < EXPANDED_BREAKPOINT) {
            onNodeWithContentDescription("返回会话").performClick()
        } else {
            onNode(hasText("工作台") and hasClickAction()).performClick()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(NARROW_WIDTH), arrayOf(WIDE_WIDTH))
    }
}

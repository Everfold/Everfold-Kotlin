package com.everfold.kotlin.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.everfold.kotlin.feature.connections.ClusterSetup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ConversationHeaderTest(
    private val width: Int,
) {
    @Test
    fun headerStaysOneRowAndShowsLiveConnectionStatus() =
        workspaceUiTest(width) { fixture ->
            openConversationIfCompact(width)
            onNodeWithContentDescription("连接状态：已连接").assertIsDisplayed()
            val longName = "Office connection with a very long name that belongs in the menu"
            val id =
                runOnIdle {
                    assertNotNull(fixture.workspace.connections.add(longName, ClusterSetup())).also {
                        fixture.workspace.selectNewConversationConnection(it)
                    }
                }
            val indicator = onNodeWithContentDescription("连接状态：未连接").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val create = onNodeWithContentDescription("新建会话").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val menu = onNodeWithContentDescription("会话菜单").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertEquals(create.center.y, indicator.center.y, absoluteTolerance = 1f)
            assertEquals(create.center.y, menu.center.y, absoluteTolerance = 1f)
            assertTrue(menu.bottom <= 56f, "Header fits in a single 56 dp row")
            assertTrue(indicator.right < create.left)
            assertTrue(create.right <= menu.left)
            onNodeWithText("$longName · 未连接").assertDoesNotExist()
            onNodeWithText("默认工作区").assertDoesNotExist()
            runOnIdle { fixture.workspace.connections.connect(id) }
            onNodeWithContentDescription("连接状态：连接中").assertIsDisplayed()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithContentDescription("连接状态：已连接").assertIsDisplayed()
            onNodeWithContentDescription("会话菜单").performClick()
            onNodeWithText("$longName · 已连接").assertIsDisplayed()
            onNodeWithText("默认工作区\n/workspace").assertIsDisplayed()
        }

    @Test
    fun menuContainsContextAndAllowsReturningBetweenSelectors() =
        workspaceUiTest(width) { fixture ->
            openConversationIfCompact(width)
            onNodeWithContentDescription("会话菜单").performClick()
            onNodeWithText("本机 · 已连接").assertIsDisplayed()
            if (width < 600) {
                val title = onNodeWithText("会话菜单", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val connection =
                    onNode(
                        hasText("连接") and hasAnySibling(hasText("本机 · 已连接")),
                        useUnmergedTree = true,
                    ).fetchSemanticsNode().boundsInRoot
                assertTrue(title.bottom <= connection.top)
            }
            onNode(hasText("连接") and hasText("本机 · 已连接") and hasClickAction()).performClick()
            onNodeWithText("管理连接").assertIsDisplayed()
            onNodeWithText("返回").performClick()
            onNode(hasText("工作区") and hasClickAction()).performClick()
            if (width < 600) {
                val title = onNodeWithText("选择工作区", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val option = onNodeWithText("Notes", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertTrue(title.bottom <= option.top)
            }
            onNodeWithText("Notes").performClick()
            runOnIdle {
                assertEquals(
                    "/workspace/notes",
                    fixture.workspace.newConversation.workspace
                        ?.path,
                )
            }
            onNodeWithContentDescription("会话菜单").performClick()
            onNodeWithText("Notes\n/workspace/notes").assertIsDisplayed()
        }

    @Test
    fun boundWorkspaceIsReadableWithoutACopyButton() =
        workspaceUiTest(width) { fixture ->
            openConversationIfCompact(width)
            onNode(hasSetTextAction() and hasText("输入消息…")).performTextReplacement("Workspace details")
            onNodeWithContentDescription("发送").performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithContentDescription("会话菜单").performClick()
            onNodeWithText("默认工作区\n/workspace").assertIsDisplayed().assertIsEnabled()
            onNodeWithText("复制路径").assertDoesNotExist()
        }

    @Test
    fun longConnectionListsCanBeSearchedWithoutHidingNavigation() =
        workspaceUiTest(width) { fixture ->
            openConversationIfCompact(width)
            val target =
                runOnIdle {
                    (1..10).map { assertNotNull(fixture.workspace.connections.add("Office $it", ClusterSetup())) }.last()
                }
            onNodeWithContentDescription("会话菜单").performClick()
            onNode(hasText("连接") and hasText("已连接", substring = true) and hasClickAction()).performClick()
            onNode(hasSetTextAction() and hasText("搜索选项")).performTextReplacement("missing")
            onNodeWithText("没有匹配的选项").assertIsDisplayed()
            onNodeWithText("返回").assertExists()
            onNodeWithText("管理连接").assertExists()
            onNode(hasSetTextAction() and hasText("missing")).performTextReplacement("Office 10")
            onNodeWithText("Office 10 · 未连接").performClick()
            runOnIdle { assertEquals(target, fixture.workspace.newConversation.connectionId) }
            onNodeWithContentDescription("会话菜单").performClick()
            onNode(hasText("连接") and hasText("Office 10 · 未连接") and hasClickAction()).performClick()
            onNodeWithText("本机 · 已连接").assertExists()
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(320), arrayOf(390), arrayOf(1280))
    }
}

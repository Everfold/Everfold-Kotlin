package com.everfold.kotlin.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import com.everfold.kotlin.feature.connections.ClusterSetup
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
@RunWith(Parameterized::class)
class ConversationHeaderTest(
    private val width: Int,
) {
    @Test
    fun selectorsShareOneRowAndTypographyEvenWithLongNames() =
        workspaceUiTest(width) { fixture ->
            val connection = onNodeWithText("本机 · 已连接", useUnmergedTree = true).layout()
            val workspace = onNodeWithText("默认工作区", useUnmergedTree = true).layout()
            assertEquals(connection.layoutInput.style.fontSize, workspace.layoutInput.style.fontSize)
            assertEquals(connection.layoutInput.style.fontWeight, workspace.layoutInput.style.fontWeight)
            val longName = "Office connection with a very long name that should be truncated"
            runOnIdle {
                val id = assertNotNull(fixture.workspace.connections.add(longName, ClusterSetup()))
                fixture.workspace.selectNewConversationConnection(id)
            }
            val left = onNodeWithContentDescription("选择连接").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val right = onNodeWithContentDescription("选择工作区").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertEquals(left.center.y, right.center.y, absoluteTolerance = 1f)
            assertTrue(left.right <= right.left)
            assertTrue(right.right <= width)
            assertEquals(1, onNodeWithText("$longName · 未连接", useUnmergedTree = true).layout().lineCount)
            onNodeWithContentDescription("新建会话").assertIsDisplayed()
        }

    @Test
    fun bottomSheetTitlesAndOptionTextHaveTheSameInset() =
        workspaceUiTest(width) {
            if (width < 600) {
                onNodeWithContentDescription("选择连接").performClick()
                val connectionTitle = onNodeWithText("选择连接", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                val manage = onNodeWithText("管理连接", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertEquals(connectionTitle.left, manage.left, absoluteTolerance = 1f)
                onNode(hasText("本机 · 已连接") and !hasContentDescription("选择连接")).performClick()
                onNodeWithContentDescription("选择工作区").performClick()
                val workspaceTitle = onNodeWithText("选择工作区", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                val option = onNodeWithText("Notes", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                assertEquals(workspaceTitle.left, option.left, absoluteTolerance = 1f)
                onNodeWithText("Notes").performClick()
            }
        }

    private fun SemanticsNodeInteraction.layout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(320), arrayOf(390), arrayOf(1280))
    }
}

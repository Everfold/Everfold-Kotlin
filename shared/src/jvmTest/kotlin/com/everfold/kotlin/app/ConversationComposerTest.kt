package com.everfold.kotlin.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.everfold.kotlin.feature.conversation.AttachmentSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ConversationComposerTest(
    private val width: Int,
) {
    @Test
    fun selectorsAndAttachmentOnlySendingWorkAtBothWidths() =
        workspaceUiTest(width) { fixture ->
            onNodeWithContentDescription("会话菜单").performClick()
            onNode(hasText("工作区") and hasClickAction()).performClick()
            onNodeWithText("Notes").performClick()
            onNodeWithContentDescription("选择模型").performClick()
            onNodeWithText("推理模型").performClick()
            runOnIdle { fixture.fileResult = Result.success(listOf(AttachmentSource("brief.pdf", "file:///brief.pdf"))) }
            onNodeWithContentDescription("上传文件").performClick()
            onNodeWithText("brief.pdf").assertIsDisplayed()
            onNodeWithText("上传中").assertIsDisplayed()
            onNodeWithContentDescription("发送").assertIsNotEnabled()
            val inputBounds = onNode(hasSetTextAction() and hasText("输入消息…")).fetchSemanticsNode().boundsInRoot
            val sendBounds = onNodeWithContentDescription("发送").fetchSemanticsNode().boundsInRoot
            assertTrue(sendBounds.right >= inputBounds.right - sendBounds.width, "Send stays at the right edge of the composer")
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithContentDescription("发送").assertIsEnabled().performClick()
            onNodeWithContentDescription("选择模型").assertIsNotEnabled()
            runOnIdle {
                val session = fixture.workspace.conversations.first { it.messages.any { message -> message.attachments.isNotEmpty() } }
                assertEquals("/workspace/notes", session.workspace?.path)
                assertTrue(
                    session.messages
                        .first()
                        .model
                        ?.id
                        ?.endsWith("/reasoning") == true,
                )
                fixture.scheduler.advanceUntilIdle()
            }
            onNodeWithContentDescription("选择模型").assertIsEnabled()
            onNodeWithContentDescription("移除附件 brief.pdf").assertDoesNotExist()
            onNode(hasText("brief.pdf") and hasAnyAncestor(hasScrollAction()) and !hasClickAction()).assertIsDisplayed()
            onNodeWithContentDescription("会话菜单").performClick()
            onNode(hasText("工作区") and hasClickAction()).assertDoesNotExist()
            onNodeWithText("复制路径").assertIsDisplayed()
            onNodeWithText("默认工作区").assertDoesNotExist()
            onNodeWithText("Notes\n/workspace/notes").assertIsDisplayed()
        }

    @Test
    fun changingWorkspaceClearsFilesAndPickerFailureCanBeDismissed() =
        workspaceUiTest(width) { fixture ->
            onNode(hasSetTextAction() and hasText("输入消息…")).performTextReplacement("Keep my draft")
            runOnIdle { fixture.fileResult = Result.success(listOf(AttachmentSource("draft.txt", "file:///draft.txt"))) }
            onNodeWithContentDescription("上传文件").performClick()
            onNodeWithContentDescription("会话菜单").performClick()
            onNode(hasText("工作区") and hasClickAction()).performClick()
            onNodeWithText("Notes").performClick()
            onNodeWithText("draft.txt").assertDoesNotExist()
            onNode(hasSetTextAction() and hasText("Keep my draft")).assertIsDisplayed()
            runOnIdle { fixture.fileResult = Result.failure(IllegalStateException("Picker unavailable")) }
            onNodeWithContentDescription("上传文件").performClick()
            onNodeWithText("无法打开或读取所选文件，请重试。").assertIsDisplayed()
            onNodeWithContentDescription("关闭文件提示").performClick()
            onNodeWithText("无法打开或读取所选文件，请重试。").assertDoesNotExist()
        }

    @Test
    fun expandedEditorKeepsTheDraftAndGivesLongTextMoreRoom() =
        workspaceUiTest(width) { fixture ->
            val draft = (1..12).joinToString("\n") { "Draft line $it" }
            onNodeWithContentDescription("展开编辑").assertDoesNotExist()
            onNode(hasSetTextAction() and hasText("输入消息…")).performTextReplacement(draft)
            val compact = onNode(hasSetTextAction() and hasText(draft)).fetchSemanticsNode().boundsInRoot
            onNodeWithContentDescription("展开编辑").performClick()
            val expanded = onNode(hasSetTextAction() and hasText(draft)).fetchSemanticsNode().boundsInRoot
            assertTrue(expanded.height > compact.height * 2)
            onNodeWithContentDescription("收起编辑").performClick()
            onNode(hasSetTextAction() and hasText(draft)).assertTextContains(draft)
            runOnIdle { assertEquals(draft, fixture.workspace.newConversation.draft) }
            onNodeWithContentDescription("展开编辑").performClick()
            onNodeWithContentDescription("发送").performClick()
            onNodeWithContentDescription("收起编辑").assertDoesNotExist()
            runOnIdle {
                assertEquals(
                    draft,
                    fixture.workspace.conversations
                        .first()
                        .messages
                        .first()
                        .text,
                )
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(390), arrayOf(1280))
    }
}

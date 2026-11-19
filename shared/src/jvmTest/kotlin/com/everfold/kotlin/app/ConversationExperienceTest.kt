package com.everfold.kotlin.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.conversation.AttachmentSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ConversationExperienceTest(
    private val width: Int,
) {
    @Test
    fun replyWidthIsBoundedAndStatusBelongsToTheReply() =
        workspaceUiTest(width) { fixture ->
            openConversationIfCompact(width)
            val reply = "A readable reply with enough words to fill a line. ".repeat(6).trim()
            runOnIdle { fixture.workspace.replyText = reply }
            onNode(hasSetTextAction() and !hasText("搜索")).performTextReplacement("Explain this")
            onNodeWithContentDescription("发送").performClick()
            onNode(hasContentDescription("执行详情") and hasAnyAncestor(hasScrollAction())).assertIsDisplayed()
            val partialReply =
                runOnIdle {
                    fixture.scheduler.advanceTimeBy(600)
                    fixture.scheduler.runCurrent()
                    fixture.workspace.conversations
                        .first()
                        .messages
                        .last()
                        .text
                }
            val author = onNodeWithText("本机 · 通用模型").fetchSemanticsNode().boundsInRoot
            val running = onNodeWithContentDescription("执行详情").fetchSemanticsNode().boundsInRoot
            val progress =
                onNode(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo),
                    useUnmergedTree = true,
                ).fetchSemanticsNode().boundsInRoot
            val partialBody = onNodeWithText(partialReply).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(running.top >= author.bottom)
            assertEquals(running.center.y, progress.center.y, absoluteTolerance = 1f)
            assertTrue(running.height <= 28f && running.bottom <= partialBody.top, "Running status stays on one compact line")
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            val body = onNodeWithText(reply).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val input = onNode(hasSetTextAction() and !hasText("搜索")).fetchSemanticsNode().boundsInRoot
            val status = onNodeWithContentDescription("执行详情").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(body.width <= 792f, "Reply has a bounded reading width")
            assertEquals(input.center.x, body.center.x, absoluteTolerance = 2f)
            assertTrue(status.top >= body.bottom, "Status follows the reply instead of occupying the header")
            onNodeWithText("1. 整理输入").assertDoesNotExist()
            onNodeWithContentDescription("执行详情").performClick()
            onNodeWithText("1. 整理输入").assertIsDisplayed()
        }

    @Test
    fun stoppingBeforeTheFirstTokenStillOffersContinueInTheMessageList() =
        workspaceUiTest(width) { fixture ->
            openConversationIfCompact(width)
            onNode(hasSetTextAction() and !hasText("搜索")).performTextReplacement("Start a reply")
            onNodeWithContentDescription("发送").performClick()
            onNodeWithContentDescription("暂停").performClick()
            onNode(hasText("继续执行") and hasAnyAncestor(hasScrollAction())).assertIsDisplayed().performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithText("Preview reply").assertIsDisplayed()
            onNodeWithContentDescription("执行详情").assertIsDisplayed()
        }

    @Test
    fun uploadFailuresCanBeRecoveredWithoutLeavingTheDraft() =
        workspaceUiTest(width) { fixture ->
            openConversationIfCompact(width)
            runOnIdle { fixture.fileResult = Result.success(listOf(AttachmentSource("draft.txt", "file:///draft.txt"))) }
            onNode(hasSetTextAction() and !hasText("搜索")).performTextReplacement("Keep this draft")
            onNodeWithContentDescription("上传文件").performClick()
            onNodeWithText("上传中").assertIsDisplayed()
            onNodeWithText("附件正在上传，完成后即可发送。").assertDoesNotExist()
            onNodeWithContentDescription("发送").assertIsNotEnabled()
            runOnIdle {
                fixture.workspace.connections.disconnect(BUILT_IN_CONNECTION_ID)
                fixture.scheduler.advanceUntilIdle()
            }
            val notice = onNodeWithText("连接已断开").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val attachment = onNodeWithText("draft.txt").fetchSemanticsNode().boundsInRoot
            assertTrue(notice.bottom <= attachment.top && attachment.top - notice.bottom < 100f)
            onNodeWithText("重新连接").performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithText("附件上传失败").assertIsDisplayed()
            onNodeWithText("重试上传").performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithContentDescription("发送").assertIsEnabled()
            onNodeWithText("draft.txt").assertIsDisplayed()
            onNodeWithText("已就绪").assertDoesNotExist()
            onNodeWithText("上传中").assertDoesNotExist()
            onNode(hasSetTextAction() and hasText("Keep this draft")).assertIsDisplayed()
            runOnIdle {
                assertTrue(
                    fixture.workspace.newConversation.messages
                        .isEmpty(),
                )
            }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(390), arrayOf(1600))
    }
}

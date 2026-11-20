package com.everfold.kotlin.app

import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ConversationLayoutSettingsTest(
    private val width: Int,
) {
    @Test
    fun marginsSettingResizesMessagesAndComposerWithoutLosingTheDraft() =
        workspaceUiTest(width) { fixture ->
            val reply = "A reply that fills the available reading width. ".repeat(6).trim()
            runOnIdle { fixture.workspace.replyText = reply }
            onNode(hasSetTextAction() and !hasText("搜索")).performTextReplacement("Start a conversation")
            onNodeWithContentDescription("发送").performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNode(hasSetTextAction() and !hasText("搜索")).performTextReplacement("Keep my draft")
            val originalReply = onNodeWithText(reply).fetchSemanticsNode().boundsInRoot
            val originalInput = onNode(hasSetTextAction() and hasText("Keep my draft")).fetchSemanticsNode().boundsInRoot

            openConversationSettings()
            onNodeWithText("两侧留白").performScrollTo().assertIsOn().performClick()
            onNodeWithText("两侧留白").assertIsOff()
            returnToConversation()
            val fullReply = onNodeWithText(reply).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val fullInput = onNode(hasSetTextAction() and hasText("Keep my draft")).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            if (width > 840) {
                assertTrue(fullReply.width > originalReply.width)
                assertTrue(fullInput.width > originalInput.width)
            } else {
                assertEquals(originalReply.width, fullReply.width, absoluteTolerance = 1f)
            }
            assertEquals(fullInput.center.x, fullReply.center.x, absoluteTolerance = 2f)

            openConversationSettings()
            onNodeWithText("两侧留白").performScrollTo().assertIsOff().performClick()
            returnToConversation()
            assertEquals(originalReply.width, onNodeWithText(reply).fetchSemanticsNode().boundsInRoot.width, absoluteTolerance = 1f)
            assertEquals(
                originalInput.width,
                onNode(hasSetTextAction() and hasText("Keep my draft")).fetchSemanticsNode().boundsInRoot.width,
                absoluteTolerance = 1f,
            )
        }

    private fun DesktopComposeUiTest.openConversationSettings() {
        if (width < 900) onNodeWithContentDescription("打开工作台").performClick()
        onNode(hasText("设置") and hasClickAction()).performClick()
        onNode(hasText("会话") and hasClickAction()).performClick()
    }

    private fun DesktopComposeUiTest.returnToConversation() {
        if (width < 900) {
            onNodeWithContentDescription("返回会话").performClick()
        } else {
            onNode(hasText("工作台") and hasClickAction()).performClick()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(390), arrayOf(1600))
    }
}

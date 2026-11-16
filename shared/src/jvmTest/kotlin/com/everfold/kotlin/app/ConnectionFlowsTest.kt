package com.everfold.kotlin.app

import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionStatus
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ConnectionFlowsTest(
    private val width: Int,
) {
    @Test
    fun savedConnectionsCanBeEditedWithoutConnecting() =
        workspaceUiTest(width) { fixture ->
            openConnections()
            addConnection("Office")
            val id =
                runOnIdle {
                    fixture.workspace.connections.connections
                        .last()
                        .id
                }
            onNodeWithText("在此新建会话").assertIsNotEnabled()
            onNodeWithText("配置").performClick()
            nameField().performTextReplacement("  ")
            onNodeWithText("保存").performScrollTo().performClick()
            onNodeWithText("请输入连接名称").assertIsDisplayed()
            runOnIdle {
                assertEquals(
                    UiText.Literal("Office"),
                    fixture.workspace.connections
                        .find(id)
                        ?.profile
                        ?.name,
                )
            }
            nameField().performTextReplacement("Renamed")
            onNodeWithText("保存").performScrollTo().performClick()
            onNodeWithText("配置已保存").assertIsDisplayed()
            nameField().performTextReplacement("Discard this")
            onNodeWithText("还原修改").performScrollTo().performClick()
            nameField().assertTextContains("Renamed")
            runOnIdle {
                assertEquals(
                    ConnectionStatus.Disconnected,
                    fixture.workspace.connections
                        .find(id)
                        ?.runtime
                        ?.status,
                )
                assertEquals(
                    BUILT_IN_CONNECTION_ID,
                    fixture.workspace.connections.connections
                        .first()
                        .id,
                )
            }
        }

    @Test
    fun unsavedDraftSurvivesReturningToTheConversationAndReopeningDetails() =
        workspaceUiTest(width) {
            openConnections()
            addConnection("Office")
            onNodeWithText("配置").performClick()
            nameField().performTextReplacement("Unfinished edit")
            onNodeWithContentDescription("返回").performClick()
            onNode(hasText("Office") and hasClickAction()).assertIsDisplayed()
            if (width < 900) {
                onNodeWithContentDescription("返回").performClick()
            } else {
                onNode(hasText("工作台") and hasClickAction()).performClick()
            }
            openConnections()
            onNode(hasText("Office") and hasClickAction()).performClick()
            nameField().assertTextContains("Unfinished edit")
        }

    @Test
    fun defaultNameFollowsLanguageAfterSavingWhileCustomNamesStayLiteral() =
        workspaceUiTest(width) { fixture ->
            openConnections()
            onNode(hasText("本机") and hasClickAction()).performClick()
            onNodeWithText("配置").performClick()
            onNodeWithText("保存").performScrollTo().performClick()
            runOnIdle { fixture.language = AppLanguage.English }
            onNode(hasSetTextAction() and hasText("Connection name")).assertTextContains("This device")
            runOnIdle {
                assertIs<UiText.Resource>(
                    fixture.workspace.connections
                        .find(BUILT_IN_CONNECTION_ID)
                        ?.profile
                        ?.name,
                )
            }
            onNode(hasSetTextAction() and hasText("Connection name")).performTextReplacement("My device")
            onNodeWithText("Save").performScrollTo().performClick()
            runOnIdle { fixture.language = AppLanguage.Chinese }
            nameField().assertTextContains("My device")
        }

    @Test
    fun offlineDraftCannotSendAndConnectedSessionKeepsItsBindingWhenDisconnected() =
        workspaceUiTest(width) { fixture ->
            val id = runOnIdle { assertNotNull(fixture.workspace.connections.add("Office", ClusterSetup())) }
            onNodeWithText("本机 · 已连接").performClick()
            onNodeWithText("Office · 未连接").performClick()
            onNode(hasSetTextAction() and hasText("在思考什么问题呢......")).performTextReplacement("Keep this question")
            onNodeWithContentDescription("发送").assertIsNotEnabled()
            onNodeWithText("管理连接").performClick()
            onNode(hasText("连接") and hasAnySibling(hasText("在此新建会话"))).performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithText("在此新建会话").assertIsEnabled().performClick()
            onNode(hasSetTextAction() and hasText("Keep this question")).assertIsDisplayed()
            onNodeWithContentDescription("发送").assertIsEnabled().performClick()
            val session = runOnIdle { fixture.workspace.conversations.first { it.connectionId == id } }
            runOnIdle {
                fixture.workspace.connections.disconnect(id)
                fixture.scheduler.advanceUntilIdle()
            }
            onNodeWithText("连接尚未就绪，消息和草稿会保留。").assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("在思考什么问题呢......")).performTextReplacement("Next question")
            onNodeWithContentDescription("发送").assertIsNotEnabled()
            runOnIdle {
                val stopped = fixture.workspace.conversations.first { it.id == session.id }
                assertEquals(id, stopped.connectionId)
                assertEquals("Next question", stopped.draft)
                assertTrue(stopped.messages.any { it.text == "Keep this question" })
                assertTrue(
                    fixture.workspace.connections
                        .find(BUILT_IN_CONNECTION_ID)
                        ?.isConnected == true,
                )
            }
        }

    @Test
    fun aFailedConnectionCanRetryAndRemovalRequiresConfirmation() =
        workspaceUiTest(width) { fixture ->
            openConnections()
            addConnection("Office")
            val id =
                runOnIdle {
                    fixture.workspace.connections.connections
                        .last()
                        .id
                }
            onNodeWithText("模拟测试结果 · 成功").performScrollTo().performClick()
            onNodeWithText("连接超时").performClick()
            onNode(hasText("连接") and hasAnySibling(hasText("在此新建会话"))).performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithText("在此新建会话").assertIsNotEnabled()
            runOnIdle {
                assertEquals(
                    ConnectionStatus.Failed,
                    fixture.workspace.connections
                        .find(id)
                        ?.runtime
                        ?.status,
                )
            }
            onNodeWithText("模拟测试结果 · 连接超时").performScrollTo().performClick()
            onNodeWithText("成功").performClick()
            onNodeWithText("重试连接").performClick()
            runOnIdle { fixture.scheduler.advanceUntilIdle() }
            onNodeWithText("在此新建会话").assertIsEnabled()
            onNodeWithText("移除连接").performScrollTo().performClick()
            onNodeWithText("取消").performClick()
            runOnIdle { assertNotNull(fixture.workspace.connections.find(id)) }
            onNodeWithText("移除连接").performClick()
            onNode(hasText("移除连接") and hasAnyAncestor(isDialog())).performClick()
            runOnIdle {
                assertEquals(
                    listOf(BUILT_IN_CONNECTION_ID),
                    fixture.workspace.connections.connections
                        .map { it.id },
                )
            }
            onNode(hasText("本机") and hasClickAction()).assertIsDisplayed()
        }

    private fun DesktopComposeUiTest.openConnections() {
        if (width < 900) onNodeWithContentDescription("打开工作台").performClick()
        onNode(hasText("连接") and hasClickAction()).performClick()
    }

    private fun DesktopComposeUiTest.addConnection(name: String) {
        onNodeWithText("添加连接").performClick()
        nameField().performTextReplacement(name)
        onNodeWithText("保存").performScrollTo().performClick()
    }

    private fun DesktopComposeUiTest.nameField() = onNode(hasSetTextAction() and hasText("连接名称"))

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(390), arrayOf(1280))
    }
}

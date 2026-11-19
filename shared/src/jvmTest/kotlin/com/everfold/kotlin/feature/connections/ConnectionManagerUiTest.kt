package com.everfold.kotlin.feature.connections

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.AppLanguageEnvironment
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.theme.EverfoldTheme
import com.everfold.kotlin.ui.theme.LocalBackdropBlurEnabled
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class ConnectionManagerUiTest {
    @Test
    fun detailsUseTheInjectedManagerAndObserveItsAsynchronousResults() {
        val originalLocale = Locale.getDefault()
        try {
            runDesktopComposeUiTest(testTimeout = 30.seconds) {
                val manager = ControlledConnections()
                val navigation = ConnectionNavigationState("external")
                var newSessionConnection: String? = null
                setContent {
                    AppLanguageEnvironment(AppLanguage.Chinese) {
                        CompositionLocalProvider(LocalBackdropBlurEnabled provides false) {
                            EverfoldTheme {
                                ConnectionsScreen(
                                    manager,
                                    navigation,
                                    onNewConversation = { newSessionConnection = it },
                                    onBack = {},
                                )
                            }
                        }
                    }
                }
                onNodeWithText("模拟测试结果", substring = true).assertDoesNotExist()
                onNodeWithText("在此新建会话").assertIsNotEnabled()
                onNode(hasText("连接") and hasClickAction()).performClick()
                runOnIdle {
                    assertEquals(listOf("connect:external"), manager.actions)
                    manager.publish(ConnectionRuntimeState(ConnectionStatus.Failed, ConnectionFailure.Timeout))
                }
                onNodeWithText("重试连接").performClick()
                runOnIdle {
                    assertEquals(listOf("connect:external", "connect:external"), manager.actions)
                    manager.publish(ConnectionRuntimeState(ConnectionStatus.Connected, kernelVersion = "External runtime"))
                }
                onNodeWithText("External runtime").assertExists()
                onNodeWithText("在此新建会话").assertIsEnabled().performClick()
                runOnIdle { assertEquals("external", newSessionConnection) }
                onNodeWithText("断开").performClick()
                runOnIdle { assertEquals("disconnect:external", manager.actions.last()) }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }
}

/** 只发布测试指定的状态不使用模拟连接的协程或延迟规则 */
private class ControlledConnections : ConnectionManager {
    override var connections by mutableStateOf(
        listOf(Connection(ConnectionProfile("external", UiText.Literal("External"), ClusterSetup()))),
    )
        private set
    val actions = mutableListOf<String>()

    fun publish(runtime: ConnectionRuntimeState) {
        connections = connections.map { it.copy(runtime = runtime) }
    }

    override fun connect(id: String) {
        actions += "connect:$id"
    }

    override fun disconnect(id: String) {
        actions += "disconnect:$id"
    }

    override fun remove(id: String) = error("Unexpected removal: $id")

    override fun add(
        name: String,
        setup: ClusterSetup,
        autoConnect: Boolean,
    ): String? = error("Unexpected addition: $name")

    override fun save(
        id: String,
        name: String?,
        setup: ClusterSetup?,
        autoConnect: Boolean,
    ): Boolean = error("Unexpected save: $id")
}

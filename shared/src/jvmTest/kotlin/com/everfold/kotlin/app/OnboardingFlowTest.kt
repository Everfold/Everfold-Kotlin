package com.everfold.kotlin.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.everfold.kotlin.EverfoldRoot
import com.everfold.kotlin.mock.MockConnectionOptions
import com.everfold.kotlin.mock.MockWorkspace
import com.everfold.kotlin.preferences.MemoryPreferenceStore
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.localization.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private const val NARROW_WIDTH = 390
private const val WIDE_WIDTH = 1280
private const val TEST_HEIGHT = 900
private const val TEST_TIMEOUT_SECONDS = 30
private const val CONNECTION_TEST_DELAY_ADVANCE_MS = 1_000L
private const val ONBOARDING_WIDE_THRESHOLD = 1_000

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class OnboardingFlowTest(
    private val width: Int,
) {
    @Test
    fun localOnboardingCompletesAndPersistsAcrossReentry() =
        onboardingUiTest { harness ->
            onNodeWithText("“下一步，交给智能体。”").assertIsDisplayed()
            advanceToModeStep()
            onNodeWithText("进入主页").assertIsDisplayed().performClick()
            waitForIdle()
            onNodeWithContentDescription("新建会话").assertIsDisplayed()
            runOnIdle {
                assertTrue(harness.workspace.initialized)
                assertTrue(harness.preferences.value.onboardingCompleted)
            }

            harness.remountWithFreshWorkspace()
            waitForIdle()
            onNodeWithContentDescription("新建会话").assertIsDisplayed()
            onNodeWithText("“下一步，交给智能体。”").assertDoesNotExist()
        }

    @Test
    fun clusterOnboardingValidatesAndConnectsNewCluster() =
        onboardingUiTest { harness ->
            advanceToModeStep()
            onNodeWithText("集群").performClick()
            onNodeWithText("下一步").assertIsDisplayed().performClick()
            onNodeWithText("中心节点").assertIsDisplayed().performClick()

            onNode(hasSetTextAction() and hasText("IP 地址")).performScrollTo().performTextReplacement("not-an-ip")
            onNode(hasSetTextAction() and hasText("端口")).performScrollTo().performTextReplacement("70000")
            onNodeWithText("测试连接").performClick()
            onNodeWithText("请输入有效的 IPv4 或 IPv6 地址。").performScrollTo().assertIsDisplayed()
            onNodeWithText("请输入 1-65535 范围内的端口。").performScrollTo().assertIsDisplayed()

            onNode(hasSetTextAction() and hasText("IP 地址")).performScrollTo().performTextReplacement("127.0.0.1")
            onNode(hasSetTextAction() and hasText("端口")).performScrollTo().performTextReplacement("8080")
            onNodeWithText("测试连接").performClick()
            mainClock.advanceTimeBy(CONNECTION_TEST_DELAY_ADVANCE_MS)
            waitForIdle()
            onNodeWithText("连接成功").performScrollTo().assertIsDisplayed()
            onNodeWithText("进入主页").assertIsDisplayed().performClick()
            runOnIdle { harness.scheduler.advanceUntilIdle() }
            waitForIdle()

            onNodeWithContentDescription("新建会话").assertIsDisplayed()
            runOnIdle {
                assertEquals(2, harness.workspace.connections.connections.size)
                val added =
                    harness.workspace.connections.connections
                        .last()
                assertEquals(added.id, harness.workspace.newConversation.connectionId)
                assertTrue(added.isConnected)
            }
        }

    @Test
    fun backNavigationAndSidebarStepGuards() =
        onboardingUiTest {
            onNodeWithText("下一步").performClick()
            onNodeWithText("个性化设置").assertIsDisplayed()
            onNodeWithText("上一步").performClick()
            onNodeWithText("“下一步，交给智能体。”").assertIsDisplayed()
            onNodeWithText("下一步").performClick()
            onNodeWithText("个性化设置").assertIsDisplayed()
            if (width >= ONBOARDING_WIDE_THRESHOLD) {
                onNodeWithText("使用模式").assertIsNotEnabled()
                onNodeWithText("欢迎").performClick()
                onNodeWithText("“下一步，交给智能体。”").assertIsDisplayed()
            }
        }

    private fun DesktopComposeUiTest.advanceToModeStep() {
        onNodeWithText("下一步").performClick()
        onNodeWithText("个性化设置").assertIsDisplayed()
        onNodeWithText("下一步").performClick()
        onNodeWithText("本地").assertIsDisplayed()
    }

    private fun onboardingUiTest(block: DesktopComposeUiTest.(OnboardingHarness) -> Unit) {
        val originalLocale = Locale.getDefault()
        val scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        val store = ViewModelStore()
        try {
            val harness = OnboardingHarness(store, scheduler)
            runDesktopComposeUiTest(
                width = width,
                height = TEST_HEIGHT,
                effectContext = StandardTestDispatcher(TestCoroutineScheduler()),
                runTestContext = StandardTestDispatcher(TestCoroutineScheduler()),
                testTimeout = TEST_TIMEOUT_SECONDS.seconds,
            ) {
                setContent {
                    key(harness.generation) {
                        val activeWorkspace = harness.workspace
                        assertNotEquals(0, harness.generation)
                        EverfoldRoot(
                            workspace = activeWorkspace,
                            preferences = harness.preferences,
                            onSampleReply = { activeWorkspace.replyText = it },
                            onPreviewState = activeWorkspace::previewRunState,
                            connectionOverviewExtra = { MockConnectionOptions(activeWorkspace.connections, it) },
                        )
                    }
                }
                block(harness)
            }
        } finally {
            store.clear()
            scheduler.runCurrent()
            Dispatchers.resetMain()
            Locale.setDefault(originalLocale)
        }
    }

    private class OnboardingHarness(
        private val viewModelStore: ViewModelStore,
        val scheduler: TestCoroutineScheduler,
    ) {
        private val preferenceStore = MemoryPreferenceStore()
        var generation by mutableIntStateOf(1)
            private set
        var workspace by mutableStateOf(createWorkspace(generation))
            private set
        var preferences by mutableStateOf(createPreferences())
            private set

        fun remountWithFreshWorkspace() {
            val next = generation + 1
            workspace = createWorkspace(next)
            preferences = UserPreferencesState(preferenceStore)
            generation = next
        }

        private fun createWorkspace(index: Int): MockWorkspace = MockWorkspace().also { viewModelStore.put("onboarding-$index", it) }

        private fun createPreferences(): UserPreferencesState =
            UserPreferencesState(preferenceStore).apply {
                update { it.copy(language = AppLanguage.Chinese) }
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(NARROW_WIDTH), arrayOf(WIDE_WIDTH))
    }
}

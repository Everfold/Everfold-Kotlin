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
import com.everfold.kotlin.feature.onboarding.OnboardingPermissionItem
import com.everfold.kotlin.feature.onboarding.OnboardingPermissionsController
import com.everfold.kotlin.feature.onboarding.OnboardingScreen
import com.everfold.kotlin.feature.onboarding.PERMISSION_ID_LIVE_UPDATES
import com.everfold.kotlin.feature.onboarding.PERMISSION_ID_NOTIFICATIONS
import com.everfold.kotlin.mock.MockConnectionOptions
import com.everfold.kotlin.mock.MockWorkspace
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.AppLanguageEnvironment
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.theme.EverfoldTheme
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
    fun localOnboardingCompletesInSessionAndStartsFreshOnColdRelaunch() =
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

            harness.remountColdLaunch()
            waitForIdle()
            onNodeWithText("“下一步，交给智能体。”").assertIsDisplayed()
            onNodeWithContentDescription("新建会话").assertDoesNotExist()
        }

    @Test
    fun clusterOnboardingValidatesAndConnectsNewCluster() =
        onboardingUiTest { harness ->
            advanceToModeStep()
            onNodeWithText("集群").performClick()
            onNodeWithText("下一步").assertIsDisplayed().performClick()
            onNodeWithText("中心节点").assertIsDisplayed().performClick()

            onNode(hasSetTextAction() and hasText("主机或 IP 地址")).performScrollTo().performTextReplacement("not-an-ip")
            onNode(hasSetTextAction() and hasText("端口")).performScrollTo().performTextReplacement("70000")
            onNodeWithText("测试连接").performClick()
            onNodeWithText("请输入有效的域名、IPv4 或 IPv6 地址。").performScrollTo().assertIsDisplayed()
            onNodeWithText("请输入 1-65535 范围内的端口。").performScrollTo().assertIsDisplayed()

            onNode(hasSetTextAction() and hasText("主机或 IP 地址")).performScrollTo().performTextReplacement("127.0.0.1")
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

    @Test
    fun permissionsStepDisplaysOnSupportedPlatformsAndUpdatesGrantedState() {
        val originalLocale = Locale.getDefault()
        val preferences = UserPreferencesState().apply { update { it.copy(language = AppLanguage.Chinese) } }
        var completed = false
        val controller = fakePermissionsController()
        try {
            runDesktopComposeUiTest(
                width = width,
                height = TEST_HEIGHT,
                effectContext = StandardTestDispatcher(TestCoroutineScheduler()),
                runTestContext = StandardTestDispatcher(TestCoroutineScheduler()),
                testTimeout = TEST_TIMEOUT_SECONDS.seconds,
            ) {
                setContent {
                    AppLanguageEnvironment(AppLanguage.Chinese) {
                        EverfoldTheme {
                            OnboardingScreen(
                                appearance = preferences,
                                language = AppLanguage.Chinese,
                                onLanguageChange = {},
                                onComplete = { completed = true },
                                permissions = controller,
                            )
                        }
                    }
                }
                onNodeWithText("下一步").performClick()
                onNodeWithText("个性化设置").assertIsDisplayed()
                onNodeWithText("下一步").performClick()
                onNodeWithText("系统权限").assertIsDisplayed()
                onNodeWithText("通知权限").assertIsDisplayed()
                onNodeWithText("实时活动 / 流体云胶囊").assertIsDisplayed()
                onNodeWithText("允许通知").performClick()
                onNodeWithText("已开启").assertIsDisplayed()
                onNodeWithText("下一步").performClick()
                onNodeWithText("本地").assertIsDisplayed()
                onNodeWithText("进入主页").performClick()
                assertTrue(completed)
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private fun fakePermissionsController(): OnboardingPermissionsController =
        object : OnboardingPermissionsController {
            private var notifyGranted by mutableStateOf(false)
            private var liveGranted by mutableStateOf(false)
            override val items: List<OnboardingPermissionItem>
                get() =
                    listOf(
                        OnboardingPermissionItem(
                            id = PERMISSION_ID_NOTIFICATIONS,
                            title = UiText.Literal("通知权限"),
                            description = UiText.Literal("用于推送长程任务进度"),
                            granted = notifyGranted,
                            actionLabel = UiText.Literal("允许通知"),
                        ),
                        OnboardingPermissionItem(
                            id = PERMISSION_ID_LIVE_UPDATES,
                            title = UiText.Literal("实时活动 / 流体云胶囊"),
                            description = UiText.Literal("状态栏胶囊显示进度"),
                            granted = liveGranted,
                            actionLabel = UiText.Literal("前往开启"),
                        ),
                    )

            override fun request(id: String) {
                if (id == PERMISSION_ID_NOTIFICATIONS) notifyGranted = true else liveGranted = true
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
        var generation by mutableIntStateOf(1)
            private set
        var workspace by mutableStateOf(createWorkspace(generation))
            private set
        var preferences by mutableStateOf(createPreferences())
            private set

        fun remountColdLaunch() {
            val next = generation + 1
            workspace = createWorkspace(next)
            preferences = createPreferences()
            generation = next
        }

        private fun createWorkspace(index: Int): MockWorkspace = MockWorkspace().also { viewModelStore.put("onboarding-$index", it) }

        private fun createPreferences(): UserPreferencesState =
            UserPreferencesState().apply {
                update { it.copy(language = AppLanguage.Chinese) }
            }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}")
        fun widths() = listOf(arrayOf(NARROW_WIDTH), arrayOf(WIDE_WIDTH))
    }
}

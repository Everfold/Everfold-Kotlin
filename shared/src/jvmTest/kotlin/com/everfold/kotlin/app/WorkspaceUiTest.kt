package com.everfold.kotlin.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.mock.MockConnectionOptions
import com.everfold.kotlin.mock.MockWorkspace
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.files.FilePicker
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.AppLanguageEnvironment
import com.everfold.kotlin.ui.theme.EverfoldTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
internal fun workspaceUiTest(
    width: Int,
    enableLongTasks: Boolean = false,
    block: DesktopComposeUiTest.(WorkspaceFixture) -> Unit,
) {
    val originalLocale = Locale.getDefault()
    val scheduler = TestCoroutineScheduler()
    Dispatchers.setMain(StandardTestDispatcher(scheduler))
    val store = ViewModelStore()
    try {
        val fixture = WorkspaceFixture(MockWorkspace(enableLongTasks = enableLongTasks), scheduler)
        store.put("workspace", fixture.workspace)
        runDesktopComposeUiTest(
            width = width,
            height = 900,
            effectContext = StandardTestDispatcher(TestCoroutineScheduler()),
            runTestContext = StandardTestDispatcher(TestCoroutineScheduler()),
            testTimeout = 30.seconds,
        ) {
            setContent {
                AppLanguageEnvironment(fixture.language) {
                    EverfoldTheme {
                        EverfoldApp(
                            fixture.workspace,
                            fixture.language,
                            { fixture.language = it },
                            preferences = fixture.preferences,
                            onPreviewState = fixture.workspace::previewRunState,
                            filePickerProvider = { onResult -> FilePicker { onResult(fixture.fileResult) } },
                            connectionOverviewExtra = { MockConnectionOptions(fixture.workspace.connections, it) },
                        )
                    }
                }
            }
            block(fixture)
        }
    } finally {
        store.clear()
        scheduler.runCurrent()
        Dispatchers.resetMain()
        Locale.setDefault(originalLocale)
    }
}

internal class WorkspaceFixture(
    val workspace: MockWorkspace,
    val scheduler: TestCoroutineScheduler,
) {
    var preferences by mutableStateOf(UserPreferencesState())
    var language by mutableStateOf(AppLanguage.Chinese)
    var fileResult: Result<List<AttachmentSource>> = Result.success(emptyList())
}

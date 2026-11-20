package com.everfold.kotlin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.everfold.kotlin.app.EverfoldApp
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.feature.onboarding.OnboardingScreen
import com.everfold.kotlin.mock.MockConnectionOptions
import com.everfold.kotlin.mock.longtask.rememberAppWorkspace
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.preferences.rememberPreferenceStore
import com.everfold.kotlin.ui.files.FilePicker
import com.everfold.kotlin.ui.files.rememberFilePicker
import com.everfold.kotlin.ui.localization.AppLanguageEnvironment
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.EverfoldTheme
import com.everfold.kotlin.workspace.Workspace
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.sample_reply

@Composable
fun App(modifier: Modifier = Modifier) {
    val store = rememberPreferenceStore()
    val persistScope = rememberCoroutineScope()
    val preferences = remember(store, persistScope) { UserPreferencesState(store, persistScope) }
    val workspace = rememberAppWorkspace()
    EverfoldRoot(
        workspace = workspace,
        preferences = preferences,
        modifier = modifier,
        onSampleReply = { workspace.replyText = it },
        onPreviewState = workspace::previewRunState,
        connectionOverviewExtra = { MockConnectionOptions(workspace.connections, it) },
    )
}

@Composable
internal fun EverfoldRoot(
    workspace: Workspace,
    preferences: UserPreferencesState,
    modifier: Modifier = Modifier,
    onSampleReply: (String) -> Unit = {},
    onPreviewState: ((String, AgentRunState) -> Unit)? = null,
    connectionOverviewExtra: @Composable (String) -> Unit = {},
    filePickerProvider: @Composable ((Result<List<AttachmentSource>>) -> Unit) -> FilePicker = { rememberFilePicker(it) },
) {
    val language = preferences.value.language
    val themeMode = preferences.themeMode
    val themeColor = preferences.themeColor
    val initialized = preferences.value.onboardingCompleted || workspace.initialized
    AppLanguageEnvironment(language) {
        val reply = localizedString(Res.string.sample_reply)
        SideEffect { onSampleReply(reply) }
        EverfoldTheme(mode = themeMode, color = themeColor) {
            Box(modifier.fillMaxSize()) {
                if (initialized) {
                    EverfoldApp(
                        workspace,
                        language,
                        { value -> preferences.update { it.copy(language = value) } },
                        preferences = preferences,
                        onPreviewState = onPreviewState,
                        connectionOverviewExtra = connectionOverviewExtra,
                        filePickerProvider = filePickerProvider,
                    )
                } else {
                    OnboardingScreen(
                        preferences,
                        language,
                        { value -> preferences.update { it.copy(language = value) } },
                        onComplete = { setup ->
                            workspace.completeSetup(setup)
                            preferences.markOnboardingCompleted()
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}

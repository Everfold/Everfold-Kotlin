package com.everfold.kotlin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.everfold.kotlin.app.EverfoldApp
import com.everfold.kotlin.feature.onboarding.OnboardingScreen
import com.everfold.kotlin.mock.MockConnectionOptions
import com.everfold.kotlin.mock.MockWorkspace
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.preferences.rememberPreferenceStore
import com.everfold.kotlin.ui.localization.AppLanguageEnvironment
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.EverfoldTheme
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.sample_reply

@Composable
fun App(
    modifier: Modifier = Modifier,
    titleBar: @Composable () -> Unit = {},
) {
    val store = rememberPreferenceStore()
    val preferences = remember(store) { UserPreferencesState(store) }
    val workspace = viewModel { MockWorkspace().also { preferences.value.restoreAppearance(it) } }
    val language = preferences.value.language
    val themeMode = workspace.themeMode
    val themeColor = workspace.themeColor
    SideEffect { preferences.update { it.copy(themeMode = themeMode, themeColor = themeColor) } }
    AppLanguageEnvironment(language) {
        val reply = localizedString(Res.string.sample_reply)
        SideEffect { workspace.replyText = reply }
        EverfoldTheme(mode = themeMode, color = themeColor) {
            Column(modifier.fillMaxSize()) {
                titleBar()
                Box(Modifier.weight(1f)) {
                    if (workspace.initialized) {
                        EverfoldApp(
                            workspace,
                            language,
                            { value -> preferences.update { it.copy(language = value) } },
                            preferences = preferences,
                            onPreviewState = workspace::previewRunState,
                            connectionOverviewExtra = { MockConnectionOptions(workspace.connections, it) },
                        )
                    } else {
                        OnboardingScreen(
                            workspace,
                            language,
                            { value -> preferences.update { it.copy(language = value) } },
                            workspace::completeSetup,
                        )
                    }
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

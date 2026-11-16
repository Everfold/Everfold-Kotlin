package com.everfold.kotlin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.everfold.kotlin.app.EverfoldApp
import com.everfold.kotlin.feature.onboarding.OnboardingScreen
import com.everfold.kotlin.mock.MockConnectionOptions
import com.everfold.kotlin.mock.MockWorkspace
import com.everfold.kotlin.ui.localization.AppLanguage
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
    var language by rememberSaveable { mutableStateOf(AppLanguage.System) }
    val workspace = viewModel { MockWorkspace() }
    AppLanguageEnvironment(language) {
        val reply = localizedString(Res.string.sample_reply)
        SideEffect { workspace.replyText = reply }
        EverfoldTheme(mode = workspace.themeMode, color = workspace.themeColor) {
            Column(modifier.fillMaxSize()) {
                titleBar()
                Box(Modifier.weight(1f)) {
                    if (workspace.initialized) {
                        EverfoldApp(
                            workspace,
                            language,
                            { language = it },
                            onPreviewState = workspace::previewRunState,
                            connectionOverviewExtra = { MockConnectionOptions(workspace.connections, it) },
                        )
                    } else {
                        OnboardingScreen(workspace, language, { language = it }, workspace::completeSetup)
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

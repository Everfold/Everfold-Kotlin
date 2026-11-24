package com.everfold.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.android.navigation.EverfoldAppShell
import com.everfold.core.designsystem.theme.EverfoldTheme
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.liveactivity.AgentLiveActivityCoordinator
import com.everfold.core.liveactivity.LiveActivityIntentContract
import com.everfold.feature.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    private val liveActivityCoordinator: AgentLiveActivityCoordinator by inject()
    private val pendingSessionId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleLaunchIntent(intent)
        setContent {
            val settingsStore: AppSettingsStore = koinInject()
            val preferences by settingsStore.preferences.collectAsStateWithLifecycle()
            val targetSessionId by pendingSessionId.collectAsStateWithLifecycle()

            EverfoldTheme(preferences = preferences) {
                if (preferences.onboardingCompleted) {
                    EverfoldAppShell(
                        pendingSessionId = targetSessionId,
                        onConsumePendingSession = { pendingSessionId.value = null },
                    )
                } else {
                    OnboardingScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        val sessionId =
            intent?.getStringExtra(LiveActivityIntentContract.EXTRA_OPEN_SESSION_ID)?.takeIf { it.isNotBlank() }
                ?: return
        pendingSessionId.value = sessionId
        liveActivityCoordinator.onSessionOpened(sessionId)
    }
}

package com.everfold.android

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
import com.everfold.feature.onboarding.OnboardingScreen
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsStore: AppSettingsStore = koinInject()
            val preferences by settingsStore.preferences.collectAsStateWithLifecycle()

            EverfoldTheme(preferences = preferences) {
                if (preferences.onboardingCompleted) {
                    EverfoldAppShell()
                } else {
                    OnboardingScreen()
                }
            }
        }
    }
}

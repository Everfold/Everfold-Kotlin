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
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.model.ConnectionState
import com.everfold.feature.onboarding.OnboardingScreen
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsStore: AppSettingsStore = koinInject()
            val authorityGateway: AuthorityGateway = koinInject()
            val preferences by settingsStore.preferences.collectAsStateWithLifecycle()
            val connectionState by authorityGateway.connectionState.collectAsStateWithLifecycle()

            EverfoldTheme(preferences = preferences) {
                if (connectionState is ConnectionState.Connected) {
                    EverfoldAppShell()
                } else {
                    OnboardingScreen()
                }
            }
        }
    }
}

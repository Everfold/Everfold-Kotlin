package com.everfold.android

import android.app.Application
import android.content.Intent
import com.everfold.core.gateway.mock.mockGatewayModule
import com.everfold.core.liveactivity.AgentLiveActivityCoordinator
import com.everfold.core.liveactivity.AppForegroundMonitor
import com.everfold.core.liveactivity.LiveActivityIntentContract
import com.everfold.core.liveactivity.LiveActivityLaunchIntentFactory
import com.everfold.core.liveactivity.liveActivityModule
import com.everfold.core.model.AppInfo
import com.everfold.feature.nodes.nodesModule
import com.everfold.feature.onboarding.onboardingModule
import com.everfold.feature.sessions.sessionsModule
import com.everfold.feature.settings.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import timber.log.Timber

class EverfoldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        val koinApplication =
            startKoin {
                androidContext(this@EverfoldApplication)
                modules(everfoldAppModules())
            }
        val koin = koinApplication.koin
        registerActivityLifecycleCallbacks(koin.get<AppForegroundMonitor>())
        koin.get<AgentLiveActivityCoordinator>().start()
    }
}

private val appMetadataModule =
    module {
        single {
            AppInfo(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE.toLong(),
            )
        }
        single {
            val appContext = androidContext()
            LiveActivityLaunchIntentFactory { sessionId ->
                Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(LiveActivityIntentContract.EXTRA_OPEN_SESSION_ID, sessionId)
                }
            }
        }
    }

internal fun everfoldAppModules(): List<Module> =
    listOf(
        appMetadataModule,
        mockGatewayModule,
        liveActivityModule,
        onboardingModule,
        sessionsModule,
        nodesModule,
        settingsModule,
    )

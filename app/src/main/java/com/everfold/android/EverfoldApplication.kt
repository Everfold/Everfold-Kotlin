package com.everfold.android

import android.app.Application
import com.everfold.core.gateway.mock.mockGatewayModule
import com.everfold.feature.models.modelsModule
import com.everfold.feature.nodes.nodesModule
import com.everfold.feature.onboarding.onboardingModule
import com.everfold.feature.sessions.sessionsModule
import com.everfold.feature.settings.settingsModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import timber.log.Timber

class EverfoldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        startKoin {
            androidContext(this@EverfoldApplication)
            modules(everfoldAppModules())
        }
    }
}

internal fun everfoldAppModules(): List<Module> =
    listOf(
        mockGatewayModule,
        onboardingModule,
        sessionsModule,
        nodesModule,
        modelsModule,
        settingsModule,
    )

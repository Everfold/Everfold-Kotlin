package com.everfold.core.liveactivity

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val liveActivityModule =
    module {
        single { AppForegroundMonitor() }
        single { LiveActivityStateReducer() }
        single<LiveActivityPublisher> {
            AndroidLiveActivityPublisher(
                context = androidContext(),
                launchIntentFactory = get(),
            )
        }
        single {
            AgentLiveActivityCoordinator(
                context = androidContext(),
                sessionGateway = get(),
                settingsStore = get(),
                foregroundMonitor = get(),
                reducer = get(),
                publisher = get(),
            )
        }
    }

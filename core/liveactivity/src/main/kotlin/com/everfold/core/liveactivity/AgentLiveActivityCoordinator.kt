package com.everfold.core.liveactivity

import android.content.Context
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.SessionGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class AgentLiveActivityCoordinator(
    private val context: Context,
    private val sessionGateway: SessionGateway,
    private val settingsStore: AppSettingsStore,
    private val foregroundMonitor: AppForegroundMonitor,
    private val reducer: LiveActivityStateReducer,
    private val publisher: LiveActivityPublisher,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (!started.compareAndSet(false, true)) return
        LiveActivityChannels.ensureChannels(context)

        scope.launch {
            combine(
                sessionGateway.sessions,
                foregroundMonitor.isForeground,
                settingsStore.preferences,
            ) { sessions, isForeground, prefs ->
                reducer.reduce(
                    sessions = sessions,
                    isForeground = isForeground,
                    prefs = prefs.notifications,
                )
            }.collect { plan ->
                plan.sessionIdsToDismiss.forEach(publisher::dismissSession)
                plan.eventsToPost.forEach(publisher::publishEvent)
            }
        }
    }

    fun onSessionOpened(sessionId: String) {
        reducer.markSessionOpened(sessionId)
        publisher.dismissSession(sessionId)
    }
}

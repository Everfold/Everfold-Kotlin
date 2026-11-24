package com.everfold.core.liveactivity

import android.content.Context
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.SessionGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
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

    @Volatile
    private var lastCapsule: LiveActivitySnapshot? = null

    fun start() {
        if (!started.compareAndSet(false, true)) return
        LiveActivityChannels.ensureChannels(context)

        val primaryIdFlow =
            combine(
                sessionGateway.sessions,
                foregroundMonitor.isForeground,
                settingsStore.preferences,
            ) { sessions, _, _ ->
                reducer.selectPrimarySessionId(sessions)
            }.distinctUntilChanged()

        val primaryDetailFlow =
            primaryIdFlow.flatMapLatest { id ->
                if (id == null) flowOf(null) else sessionGateway.observeSession(id)
            }

        scope.launch {
            combine(
                sessionGateway.sessions,
                primaryDetailFlow,
                foregroundMonitor.isForeground,
                settingsStore.preferences,
            ) { sessions, detail, isForeground, prefs ->
                reducer.reduce(
                    sessions = sessions,
                    primaryDetail = detail,
                    isForeground = isForeground,
                    prefs = prefs.notifications,
                )
            }.collect { plan ->
                applyRenderPlan(plan)
            }
        }
    }

    fun onSessionOpened(sessionId: String) {
        reducer.markSessionOpened(sessionId)
        publisher.dismissAlert(sessionId)
        val activeCapsule = lastCapsule
        if (activeCapsule != null && activeCapsule.sessionId == sessionId && activeCapsule.phase.isTerminal) {
            lastCapsule = null
            publisher.dismissCapsule()
        }
    }

    private fun applyRenderPlan(plan: LiveActivityRenderPlan) {
        val capsule = plan.capsule
        lastCapsule = capsule
        if (capsule != null) {
            publisher.publishCapsule(capsule)
        } else {
            publisher.dismissCapsule()
        }
        plan.alerts.forEach(publisher::publishAlert)
    }
}

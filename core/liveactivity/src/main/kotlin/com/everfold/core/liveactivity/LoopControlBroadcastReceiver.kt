package com.everfold.core.liveactivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.LoopControlCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import timber.log.Timber

class LoopControlBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        val command = intent.action.toLoopCommand()
        if (sessionId.isNullOrBlank() || command == null) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                runCatching {
                    KoinPlatform.getKoin().get<SessionGateway>().controlLoop(sessionId, command)
                }.onFailure { error ->
                    Timber.w(error, "Failed to dispatch loop command %s for session %s", command, sessionId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal companion object {
        const val ACTION_PAUSE = "com.everfold.liveactivity.ACTION_PAUSE"
        const val ACTION_RESUME = "com.everfold.liveactivity.ACTION_RESUME"
        const val ACTION_STOP = "com.everfold.liveactivity.ACTION_STOP"
        const val EXTRA_SESSION_ID = "com.everfold.liveactivity.EXTRA_SESSION_ID"
    }
}

private fun String?.toLoopCommand(): LoopControlCommand? =
    when (this) {
        LoopControlBroadcastReceiver.ACTION_PAUSE -> LoopControlCommand.Pause
        LoopControlBroadcastReceiver.ACTION_RESUME -> LoopControlCommand.Resume()
        LoopControlBroadcastReceiver.ACTION_STOP -> LoopControlCommand.Stop
        else -> null
    }

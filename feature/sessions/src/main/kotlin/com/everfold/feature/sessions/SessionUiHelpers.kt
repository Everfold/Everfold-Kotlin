package com.everfold.feature.sessions

import androidx.annotation.StringRes
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.everfold.core.model.LoopState

internal fun Modifier.clearFocusOnTouch(onClear: () -> Unit): Modifier =
    pointerInput(onClear) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            onClear()
        }
    }

@StringRes
internal fun LoopState.labelRes(): Int =
    when (this) {
        LoopState.IDLE -> R.string.loop_state_idle
        LoopState.PLANNING -> R.string.loop_state_planning
        LoopState.EXECUTING_ACTION -> R.string.loop_state_executing
        LoopState.AWAITING_INPUT -> R.string.loop_state_awaiting_input
        LoopState.COMPLETED -> R.string.loop_state_completed
        LoopState.FAILED -> R.string.loop_state_failed
    }

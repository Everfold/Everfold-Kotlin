package com.everfold.kotlin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PlainTooltip
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.basic.TooltipDefaults
import top.yukonga.miuix.kmp.basic.TooltipState
import top.yukonga.miuix.kmp.basic.rememberTooltipState

private const val TOOLTIP_HOVER_DELAY_MILLIS = 500L

private class TooltipHoverController(
    private val scope: CoroutineScope,
    private val state: TooltipState,
) {
    private var hoverJob: Job? = null

    fun schedule() {
        hoverJob?.cancel()
        hoverJob =
            scope.launch {
                delay(TOOLTIP_HOVER_DELAY_MILLIS)
                state.show()
            }
    }

    fun dismiss() {
        hoverJob?.cancel()
        hoverJob = null
        state.dismiss()
    }

    fun dispose() {
        dismiss()
        state.onDispose()
    }
}

/** 共享文字提示封装：悬停延迟展示，并在点击或离开时立即收起 */
@Composable
internal fun AppTooltip(
    label: String,
    content: @Composable () -> Unit,
) {
    val state = rememberTooltipState()
    val scope = rememberCoroutineScope()
    val controller = remember(state, scope) { TooltipHoverController(scope, state) }
    DisposableEffect(controller) { onDispose { controller.dispose() } }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = state,
        enableUserInput = false,
    ) {
        Box(
            Modifier.pointerInput(controller) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Enter -> controller.schedule()
                            PointerEventType.Exit, PointerEventType.Press -> controller.dismiss()
                        }
                    }
                }
            },
        ) {
            content()
        }
    }
}

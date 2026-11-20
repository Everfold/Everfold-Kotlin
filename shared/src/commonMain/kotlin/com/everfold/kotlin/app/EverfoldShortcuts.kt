package com.everfold.kotlin.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

internal fun Modifier.everfoldShortcuts(
    canGoBack: Boolean,
    wide: Boolean,
    drawerOpen: Boolean,
    handlers: ShortcutHandlers,
): Modifier =
    onPreviewKeyEvent { event ->
        handleEverfoldKeyEvent(event, canGoBack, wide, drawerOpen, handlers)
    }

private fun handleEverfoldKeyEvent(
    event: KeyEvent,
    canGoBack: Boolean,
    wide: Boolean,
    drawerOpen: Boolean,
    handlers: ShortcutHandlers,
): Boolean =
    when {
        event.type != KeyEventType.KeyDown -> false
        event.key == Key.Escape && (canGoBack || (!wide && drawerOpen)) -> handlers.onNavigateBack().let { true }
        event.isCtrlPressed || event.isMetaPressed -> handleCtrlOrMetaShortcut(event.key, handlers)
        else -> false
    }

private fun handleCtrlOrMetaShortcut(
    key: Key,
    handlers: ShortcutHandlers,
): Boolean =
    when (key) {
        Key.N -> handlers.onNewConversation().let { true }
        Key.F -> handlers.onFocusSearch().let { true }
        Key.B -> handlers.onToggleSidebar().let { true }
        else -> false
    }

internal fun handleSidebarResizeKey(
    event: KeyEvent,
    appState: EverfoldAppState,
    maxSessionWidth: Float,
): Boolean {
    val delta =
        if (event.type != KeyEventType.KeyDown) {
            null
        } else {
            when (event.key) {
                Key.DirectionLeft -> -SESSION_RESIZE_STEP_DP
                Key.DirectionRight -> SESSION_RESIZE_STEP_DP
                else -> null
            }
        }
    if (delta != null) appState.resizeSidebar(delta, maxSessionWidth)
    return delta != null
}

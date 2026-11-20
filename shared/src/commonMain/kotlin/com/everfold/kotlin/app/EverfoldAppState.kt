package com.everfold.kotlin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.everfold.kotlin.app.navigation.AppDestination
import com.everfold.kotlin.feature.connections.ConnectionNavigationState
import com.everfold.kotlin.feature.conversation.AttachmentTarget
import com.everfold.kotlin.workspace.Workspace

internal const val DEFAULT_SESSION_WIDTH_DP = 300f
internal const val MIN_SESSION_WIDTH_DP = 260f
internal const val MAX_SESSION_WIDTH_DP = 440f
internal const val SESSION_WIDTH_RESERVE_DP = 520f
internal const val SESSION_RESIZE_STEP_DP = 16f

@Stable
internal class AttachmentPickerState(
    errorTargetState: MutableState<AttachmentTarget?>,
    pendingTargetState: MutableState<AttachmentTarget?>,
) {
    var errorTarget by errorTargetState
    var pendingTarget by pendingTargetState
}

/** 顶层工作台外壳状态与导航协调器。 */
@Stable
internal class EverfoldAppState(
    val conversationStates: SaveableStateHolder,
    val sessionListState: SaveableStateHolder,
    val connectionPageStates: SaveableStateHolder,
    val connectionNavigation: ConnectionNavigationState,
    val attachments: AttachmentPickerState,
) {
    var destination by mutableStateOf(AppDestination.Sessions)
    var selectedId by mutableStateOf<String?>(null)
    var focusComposer by mutableStateOf(true)
    var sessionsVisible by mutableStateOf(true)
    var sessionWidth by mutableFloatStateOf(DEFAULT_SESSION_WIDTH_DP)
    var sessionQuery by mutableStateOf("")
    var searchRequest by mutableIntStateOf(0)
    var composerFocusRequest by mutableIntStateOf(0)

    fun openNewConversation(closeDrawer: () -> Unit) {
        focusComposer = true
        destination = AppDestination.Sessions
        sessionQuery = ""
        selectedId = null
        closeDrawer()
        composerFocusRequest++
    }

    fun selectSession(
        id: String,
        closeDrawer: () -> Unit,
    ) {
        focusComposer = true
        selectedId = id
        destination = AppDestination.Sessions
        closeDrawer()
    }

    fun deleteSession(
        workspace: Workspace,
        id: String,
    ) {
        workspace.deleteConversation(id)
        conversationStates.removeState(id)
        if (selectedId == id) selectedId = null
    }

    fun openSessionConnection(
        connectionId: String,
        exists: Boolean,
    ) {
        if (exists) {
            connectionNavigation.open(connectionId)
        } else {
            connectionNavigation.showList()
        }
        destination = AppDestination.Connections
    }

    fun navigateBack(
        wide: Boolean,
        drawerOpen: Boolean,
        closeDrawer: () -> Unit,
    ) {
        if (!wide && drawerOpen) {
            closeDrawer()
        } else if (destination != AppDestination.Connections || !connectionNavigation.back()) {
            destination = AppDestination.Sessions
        }
    }

    fun resizeSidebar(
        deltaDp: Float,
        maxSessionWidth: Float,
    ) {
        sessionWidth = (sessionWidth.coerceAtMost(maxSessionWidth) + deltaDp).coerceIn(MIN_SESSION_WIDTH_DP, maxSessionWidth)
    }

    fun startSearch() {
        focusComposer = false
        destination = AppDestination.Sessions
        sessionsVisible = true
        searchRequest++
    }

    companion object {
        private const val INDEX_DESTINATION = 0
        private const val INDEX_SELECTED_ID = 1
        private const val INDEX_FOCUS_COMPOSER = 2
        private const val INDEX_SESSIONS_VISIBLE = 3
        private const val INDEX_SESSION_WIDTH = 4
        private const val INDEX_SESSION_QUERY = 5
        private const val INDEX_SEARCH_REQUEST = 6
        private const val INDEX_COMPOSER_FOCUS_REQUEST = 7

        fun saver(
            conversationStates: SaveableStateHolder,
            sessionListState: SaveableStateHolder,
            connectionPageStates: SaveableStateHolder,
            connectionNavigation: ConnectionNavigationState,
            attachments: AttachmentPickerState,
        ): Saver<EverfoldAppState, Any> =
            listSaver(
                save = { state ->
                    listOf(
                        state.destination.name,
                        state.selectedId.orEmpty(),
                        state.focusComposer,
                        state.sessionsVisible,
                        state.sessionWidth,
                        state.sessionQuery,
                        state.searchRequest,
                        state.composerFocusRequest,
                    )
                },
                restore = { values ->
                    EverfoldAppState(
                        conversationStates,
                        sessionListState,
                        connectionPageStates,
                        connectionNavigation,
                        attachments,
                    ).apply {
                        destination = AppDestination.valueOf(values[INDEX_DESTINATION] as String)
                        selectedId = (values[INDEX_SELECTED_ID] as String).ifEmpty { null }
                        focusComposer = values[INDEX_FOCUS_COMPOSER] as Boolean
                        sessionsVisible = values[INDEX_SESSIONS_VISIBLE] as Boolean
                        sessionWidth = values[INDEX_SESSION_WIDTH] as Float
                        sessionQuery = values[INDEX_SESSION_QUERY] as String
                        searchRequest = values[INDEX_SEARCH_REQUEST] as Int
                        composerFocusRequest = values[INDEX_COMPOSER_FOCUS_REQUEST] as Int
                    }
                },
            )
    }
}

@Composable
internal fun rememberEverfoldAppState(): EverfoldAppState {
    val conversationStates = rememberSaveableStateHolder()
    val sessionListState = rememberSaveableStateHolder()
    val connectionPageStates = rememberSaveableStateHolder()
    val connectionNavigation = rememberSaveable(saver = ConnectionNavigationState.Saver) { ConnectionNavigationState() }
    val errorTargetState = rememberSaveable(stateSaver = AttachmentTarget.NullableSaver) { mutableStateOf(null) }
    val pendingTargetState = rememberSaveable(stateSaver = AttachmentTarget.NullableSaver) { mutableStateOf(null) }
    val attachments = androidx.compose.runtime.remember { AttachmentPickerState(errorTargetState, pendingTargetState) }
    return rememberSaveable(
        conversationStates,
        sessionListState,
        connectionPageStates,
        connectionNavigation,
        attachments,
        saver = EverfoldAppState.saver(conversationStates, sessionListState, connectionPageStates, connectionNavigation, attachments),
    ) {
        EverfoldAppState(conversationStates, sessionListState, connectionPageStates, connectionNavigation, attachments)
    }
}

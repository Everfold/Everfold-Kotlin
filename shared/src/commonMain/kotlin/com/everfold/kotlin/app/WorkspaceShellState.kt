package com.everfold.kotlin.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import com.everfold.kotlin.app.navigation.AppDestination
import com.everfold.kotlin.feature.connections.ConnectionNavigationState
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AttachmentTarget
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.files.FilePicker
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.workspace.Workspace

internal const val WIDE_LAYOUT_DP = 900
internal const val MIN_SESSION_WIDTH = 260f
internal const val MAX_SESSION_WIDTH = 440f
internal const val CONVERSATION_MIN_RESERVE = 520f
internal const val KEYBOARD_RESIZE_STEP = 16f

@Stable
internal class WorkspaceShellDeps(
    val workspace: Workspace,
    val language: AppLanguage,
    val onLanguageChange: (AppLanguage) -> Unit,
    val preferences: UserPreferencesState,
    val filePicker: FilePicker,
    val onPreviewState: ((String, AgentRunState) -> Unit)?,
    val connectionOverviewExtra: @Composable (String) -> Unit,
)

@Stable
internal class WorkspaceShellState(
    val sessionListState: SaveableStateHolder,
    val conversationStates: SaveableStateHolder,
    val connectionPageStates: SaveableStateHolder,
    val connectionNavigation: ConnectionNavigationState,
) {
    var fileErrorTarget by mutableStateOf<AttachmentTarget?>(null)
    var destination by mutableStateOf(AppDestination.Sessions)
    var selectedId by mutableStateOf<String?>(null)
    var focusComposer by mutableStateOf(true)
    var sessionsVisible by mutableStateOf(true)
    var sessionWidth by mutableFloatStateOf(300f)
    var sessionQuery by mutableStateOf("")
    var searchRequest by mutableIntStateOf(0)
    var composerFocusRequest by mutableIntStateOf(0)

    fun startNewConversation() {
        focusComposer = true
        destination = AppDestination.Sessions
        sessionQuery = ""
        selectedId = null
        composerFocusRequest++
    }
}

@Composable
internal fun rememberWorkspaceShellState(): WorkspaceShellState {
    val sessionListState = rememberSaveableStateHolder()
    val conversationStates = rememberSaveableStateHolder()
    val connectionPageStates = rememberSaveableStateHolder()
    val connectionNavigation = rememberSaveable(saver = ConnectionNavigationState.Saver) { ConnectionNavigationState() }
    return remember(sessionListState, conversationStates, connectionPageStates, connectionNavigation) {
        WorkspaceShellState(
            sessionListState = sessionListState,
            conversationStates = conversationStates,
            connectionPageStates = connectionPageStates,
            connectionNavigation = connectionNavigation,
        )
    }
}

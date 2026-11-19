package com.everfold.kotlin.app

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.connections.ConnectionStatusIndicator
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AttachmentButton
import com.everfold.kotlin.feature.conversation.AttachmentStatus
import com.everfold.kotlin.feature.conversation.ComposerFeedback
import com.everfold.kotlin.feature.conversation.ConversationMenu
import com.everfold.kotlin.feature.conversation.ConversationScreen
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.DraftAttachments
import com.everfold.kotlin.feature.conversation.FilePickerError
import com.everfold.kotlin.feature.conversation.ModelSelector
import com.everfold.kotlin.feature.sessions.SessionsScreen
import com.everfold.kotlin.feature.settings.SettingsScreen
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.files.FilePicker
import com.everfold.kotlin.ui.files.rememberFilePicker
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.workspace.Workspace
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_removed
import top.yukonga.miuix.kmp.basic.Scaffold

/** 主界面组装入口：宽屏使用固定侧栏，窄屏采用底栏加 miuix-nav 路由栈 */
@Composable
internal fun EverfoldApp(
    workspace: Workspace,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    onPreviewState: ((String, AgentRunState) -> Unit)? = null,
    connectionOverviewExtra: @Composable (String) -> Unit = {},
    filePicker: FilePicker = rememberFilePicker(),
    preferences: UserPreferencesState = remember { UserPreferencesState() },
) {
    val state = rememberWorkspaceShellState()
    val deps =
        WorkspaceShellDeps(
            workspace = workspace,
            language = language,
            onLanguageChange = onLanguageChange,
            preferences = preferences,
            filePicker = filePicker,
            onPreviewState = onPreviewState,
            connectionOverviewExtra = connectionOverviewExtra,
        )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .focusable()
                .imePadding(),
        ) {
            val wide = maxWidth >= WIDE_LAYOUT_DP.dp
            if (wide) {
                val maxSessionWidth =
                    (maxWidth.value - CONVERSATION_MIN_RESERVE).coerceIn(MIN_SESSION_WIDTH, MAX_SESSION_WIDTH)
                WideWorkspaceShell(deps = deps, state = state, maxSessionWidth = maxSessionWidth)
            } else {
                CompactWorkspaceShell(deps = deps, state = state)
            }
        }
    }
}

@Composable
internal fun SessionsPanel(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    compactMode: Boolean,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val selected = deps.workspace.conversations.firstOrNull { it.id == state.selectedId }
    state.sessionListState.SaveableStateProvider("sessions") {
        SessionsScreen(
            sessions = deps.workspace.conversations,
            connectionNames =
                deps.workspace.connections.connections
                    .associate { it.id to it.profile.name },
            selectedId = selected?.id,
            searchRequest = state.searchRequest,
            query = state.sessionQuery,
            onQueryChange = { state.sessionQuery = it },
            onSelect = onSelect,
            onRename = deps.workspace::renameConversation,
            onDelete = { id ->
                deps.workspace.deleteConversation(id)
                state.conversationStates.removeState(id)
                if (state.selectedId == id) state.selectedId = null
            },
            onTogglePin = deps.workspace::togglePinned,
            onCreate = onCreate,
            modifier = modifier.fillMaxSize(),
            compactMode = compactMode,
            bottomPadding = bottomPadding,
        )
    }
}

@Composable
internal fun ConversationPaneHost(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    compactNavigation: Boolean,
    onToggleSessions: () -> Unit,
    onNewConversation: () -> Unit,
    onOpenConnection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = deps.workspace.conversations.firstOrNull { it.id == state.selectedId }
    val session = selected ?: deps.workspace.newConversation
    val sessionConnection = deps.workspace.connections.find(session.connectionId)
    val options = deps.workspace.conversationOptions
    state.conversationStates.SaveableStateProvider(session.id) {
        ConversationScreen(
            state = session,
            connectionName = sessionConnection?.profile?.name?.resolve() ?: localizedString(Res.string.connection_removed),
            textSize = deps.preferences.value.textSize,
            shortcut = deps.preferences.value.shortcut,
            collapsePrompts = deps.preferences.value.collapsePrompts,
            sideMargins = deps.preferences.value.sideMargins,
            inputFocusRequest = state.composerFocusRequest,
            compactNavigation = compactNavigation,
            autoFocusInput = state.focusComposer,
            sessionsVisible = state.sessionsVisible,
            onToggleSessions = onToggleSessions,
            onNewConversation = onNewConversation,
            onDraftChange = { draft ->
                if (selected == null) deps.workspace.updateNewConversationDraft(draft) else deps.workspace.updateDraft(selected.id, draft)
            },
            onSend = { sendConversationMessage(deps, state, selected) },
            onStop = { selected?.let { deps.workspace.stop(it.id) } },
            canSend = sessionConnection?.isConnected == true,
            connectionIndicator = { ConnectionStatusIndicator(sessionConnection) },
            conversationMenu = { ConversationPaneMenu(deps, session, selected, onOpenConnection) },
            composerTools = { ConversationToolsRow(deps, state, session) },
            attachmentsContent = {
                DraftAttachments(session, options)
                if (state.fileErrorTarget == session.attachmentTarget) {
                    FilePickerError { state.fileErrorTarget = null }
                }
            },
            composerFeedback = {
                ComposerFeedback(
                    state = session,
                    connection = sessionConnection,
                    onConnect = { deps.workspace.connections.connect(session.connectionId) },
                    onOpenConnection = { onOpenConnection(session.connectionId) },
                    onRetryUploads = {
                        session.attachments
                            .filter { it.status == AttachmentStatus.Failed }
                            .forEach { options.retryAttachment(session.id, it.id) }
                    },
                )
            },
            onRetry = { selected?.let { deps.workspace.retry(it.id) } },
            modifier = modifier,
        )
    }
}

@Composable
private fun RowScope.ConversationToolsRow(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    session: ConversationUiState,
) {
    val options = deps.workspace.conversationOptions
    AttachmentButton(enabled = !session.isResponding && session.workspace != null) {
        val target = session.attachmentTarget
        state.fileErrorTarget = null
        deps.filePicker.launch { result ->
            result.onSuccess { options.attach(target, it) }.onFailure { state.fileErrorTarget = target }
        }
    }
    ModelSelector(session, options, Modifier.weight(1f))
}

@Composable
internal fun SettingsHost(
    deps: WorkspaceShellDeps,
    bottomPadding: Dp = 0.dp,
) {
    SettingsScreen(
        language = deps.language,
        onLanguageChange = deps.onLanguageChange,
        themeMode = deps.workspace.themeMode,
        themeColor = deps.workspace.themeColor,
        onThemeChange = deps.workspace::setTheme,
        onSystemColorChange = deps.workspace::useSystemThemeColor,
        onPresetChange = deps.workspace::selectThemePreset,
        onCustomColorChange = deps.workspace::applyCustomThemeColor,
        textSize = deps.preferences.value.textSize,
        onTextSizeChange = { value -> deps.preferences.update { it.copy(textSize = value) } },
        shortcut = deps.preferences.value.shortcut,
        onShortcutChange = { value -> deps.preferences.update { it.copy(shortcut = value) } },
        collapsePrompts = deps.preferences.value.collapsePrompts,
        onCollapsePromptsChange = { value -> deps.preferences.update { it.copy(collapsePrompts = value) } },
        sideMargins = deps.preferences.value.sideMargins,
        onSideMarginsChange = { value -> deps.preferences.update { it.copy(sideMargins = value) } },
        onBack = null,
        bottomPadding = bottomPadding,
    )
}

@Composable
private fun ConversationPaneMenu(
    deps: WorkspaceShellDeps,
    session: ConversationUiState,
    selected: ConversationUiState?,
    onOpenConnection: (String) -> Unit,
) {
    ConversationMenu(
        state = session,
        connections = deps.workspace.connections.connections,
        options = deps.workspace.conversationOptions,
        canSelect = selected == null,
        onSelectConnection = deps.workspace::selectNewConversationConnection,
        onOpenConnection = { onOpenConnection(session.connectionId) },
        onPreviewState = previewStateHandler(deps, selected),
    )
}

private fun previewStateHandler(
    deps: WorkspaceShellDeps,
    selected: ConversationUiState?,
): ((AgentRunState) -> Unit)? =
    if (deps.onPreviewState != null && selected != null) {
        { runState ->
            if (runState == AgentRunState.Running) {
                deps.workspace.retry(selected.id)
            } else {
                deps.onPreviewState(selected.id, runState)
            }
        }
    } else {
        null
    }

private fun sendConversationMessage(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    selected: ConversationUiState?,
) {
    if (selected == null) {
        deps.workspace.submitNewConversation()?.let { id ->
            state.selectedId = id
            state.sessionQuery = ""
            state.focusComposer = true
        }
    } else {
        deps.workspace.send(selected.id)
    }
}

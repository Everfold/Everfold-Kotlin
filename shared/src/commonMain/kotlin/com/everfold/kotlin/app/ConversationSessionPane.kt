package com.everfold.kotlin.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.everfold.kotlin.feature.connections.Connection
import com.everfold.kotlin.feature.connections.ConnectionStatusIndicator
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AttachmentButton
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.feature.conversation.AttachmentStatus
import com.everfold.kotlin.feature.conversation.ComposerFeedback
import com.everfold.kotlin.feature.conversation.ConversationMenu
import com.everfold.kotlin.feature.conversation.ConversationScreen
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.DraftAttachments
import com.everfold.kotlin.feature.conversation.FilePickerError
import com.everfold.kotlin.feature.conversation.ModelSelector
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.files.FilePicker
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.workspace.Workspace
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_removed

internal class DrawerActions(
    val open: () -> Unit,
    val close: () -> Unit,
    val navigateBack: () -> Unit,
)

internal class ShortcutHandlers(
    val onNavigateBack: () -> Unit,
    val onNewConversation: () -> Unit,
    val onFocusSearch: () -> Unit,
    val onToggleSidebar: () -> Unit,
)

internal class ShellBindings(
    val workspace: Workspace,
    val preferences: UserPreferencesState,
    val language: AppLanguage,
    val onLanguageChange: (AppLanguage) -> Unit,
    val onPreviewState: ((String, AgentRunState) -> Unit)?,
    val connectionOverviewExtra: @Composable (String) -> Unit,
    val filePicker: FilePicker,
)

@Composable
internal fun ConversationSessionPane(
    appState: EverfoldAppState,
    bindings: ShellBindings,
    selected: ConversationUiState?,
    wide: Boolean,
    drawer: DrawerActions,
    modifier: Modifier = Modifier,
) {
    val workspace = bindings.workspace
    val preferences = bindings.preferences
    val session = selected ?: workspace.newConversation
    val sessionConnection = workspace.connections.find(session.connectionId)
    val openSessionConnection = { appState.openSessionConnection(session.connectionId, sessionConnection != null) }
    ConversationScreen(
        state = session,
        connectionName = sessionConnection?.profile?.name?.resolve() ?: localizedString(Res.string.connection_removed),
        textSize = preferences.value.textSize,
        shortcut = preferences.value.shortcut,
        collapsePrompts = preferences.value.collapsePrompts,
        sideMargins = preferences.value.sideMargins,
        inputFocusRequest = appState.composerFocusRequest,
        compactNavigation = !wide,
        autoFocusInput = appState.focusComposer,
        sessionsVisible = appState.sessionsVisible,
        onToggleSessions = {
            appState.focusComposer = true
            if (wide) appState.sessionsVisible = !appState.sessionsVisible else drawer.open()
        },
        onNewConversation = { appState.openNewConversation(drawer.close) },
        onDraftChange = { value ->
            if (selected == null) workspace.updateNewConversationDraft(value) else workspace.updateDraft(selected.id, value)
        },
        onSend = { submitOrSendSession(appState, workspace, selected) },
        onStop = { selected?.let { workspace.stop(it.id) } },
        canSend = sessionConnection?.isConnected == true,
        connectionIndicator = { ConnectionStatusIndicator(sessionConnection) },
        conversationMenu = {
            SessionConversationMenu(session, selected, workspace, openSessionConnection, bindings.onPreviewState)
        },
        composerTools = {
            AttachmentButton(enabled = !session.isResponding && session.workspace != null) {
                appState.attachments.pendingTarget = session.attachmentTarget
                appState.attachments.errorTarget = null
                bindings.filePicker.launch()
            }
            ModelSelector(session, workspace.conversationOptions, Modifier.weight(1f))
        },
        attachmentsContent = {
            DraftAttachments(session, workspace.conversationOptions)
            if (appState.attachments.errorTarget == session.attachmentTarget) {
                FilePickerError { appState.attachments.errorTarget = null }
            }
        },
        composerFeedback = { SessionComposerFeedback(session, sessionConnection, workspace, openSessionConnection) },
        onRetry = { selected?.let { workspace.retry(it.id) } },
        onSelectGateOption = { optionId -> selected?.let { workspace.taskController.resolveGateOption(it.id, optionId) } },
        modifier = modifier,
    )
}

@Composable
private fun SessionConversationMenu(
    session: ConversationUiState,
    selected: ConversationUiState?,
    workspace: Workspace,
    openSessionConnection: () -> Unit,
    onPreviewState: ((String, AgentRunState) -> Unit)?,
) {
    ConversationMenu(
        session,
        workspace.connections.connections,
        workspace.conversationOptions,
        canSelect = selected == null,
        onSelectConnection = workspace::selectNewConversationConnection,
        onOpenConnection = openSessionConnection,
        onPreviewState =
            if (selected != null && onPreviewState != null) {
                { state ->
                    if (state == AgentRunState.Running) workspace.retry(selected.id) else onPreviewState(selected.id, state)
                }
            } else {
                null
            },
        onInjectFault =
            if (selected != null && onPreviewState != null) {
                { fault -> workspace.taskController.injectFault(selected.id, fault) }
            } else {
                null
            },
    )
}

@Composable
private fun SessionComposerFeedback(
    session: ConversationUiState,
    sessionConnection: Connection?,
    workspace: Workspace,
    openSessionConnection: () -> Unit,
) {
    ComposerFeedback(
        session,
        sessionConnection,
        onConnect = { workspace.connections.connect(session.connectionId) },
        onOpenConnection = openSessionConnection,
        onRetryUploads = {
            session.attachments.filter { it.status == AttachmentStatus.Failed }.forEach {
                workspace.conversationOptions.retryAttachment(session.id, it.id)
            }
        },
    )
}

private fun submitOrSendSession(
    appState: EverfoldAppState,
    workspace: Workspace,
    selected: ConversationUiState?,
) {
    if (selected == null) {
        workspace.submitNewConversation()?.let { id ->
            appState.selectedId = id
            appState.sessionQuery = ""
            appState.focusComposer = true
        }
    } else {
        workspace.send(selected.id)
    }
}

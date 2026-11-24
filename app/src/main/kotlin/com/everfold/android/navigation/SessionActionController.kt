package com.everfold.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.everfold.android.R
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionSummary
import com.everfold.feature.sessions.component.DeleteSessionDialog
import com.everfold.feature.sessions.component.RenameSessionDialog
import com.everfold.feature.sessions.component.SessionActionBottomSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
internal class SessionActionController {
    var menuTarget by mutableStateOf<AgentSessionSummary?>(null)
        private set
    var renameTarget by mutableStateOf<AgentSessionSummary?>(null)
        private set
    var deleteTarget by mutableStateOf<AgentSessionSummary?>(null)
        private set

    fun openMenu(summary: AgentSessionSummary) {
        menuTarget = summary
    }

    fun dismissMenu() {
        menuTarget = null
    }

    fun startRename() {
        renameTarget = menuTarget
        menuTarget = null
    }

    fun dismissRename() {
        renameTarget = null
    }

    fun startDelete() {
        deleteTarget = menuTarget
        menuTarget = null
    }

    fun dismissDelete() {
        deleteTarget = null
    }
}

@Composable
internal fun rememberSessionActionController(): SessionActionController = remember { SessionActionController() }

@Composable
internal fun SessionActionDialogsHost(
    controller: SessionActionController,
    sessionGateway: SessionGateway,
    scope: CoroutineScope,
    activeSessionId: String?,
    onActiveSessionChange: (String?) -> Unit,
    onShowArchiveUndo: (String, () -> Unit) -> Unit,
) {
    val archivedSnackbarMessage = stringResource(R.string.drawer_session_archived_snackbar)
    controller.menuTarget?.let { target ->
        SessionActionBottomSheet(
            summary = target,
            onDismiss = controller::dismissMenu,
            onTogglePin = {
                controller.dismissMenu()
                scope.launch { sessionGateway.setSessionPinned(target.id, pinned = !target.isPinned) }
            },
            onRename = controller::startRename,
            onToggleArchive = {
                controller.dismissMenu()
                toggleArchiveWithUndo(
                    target = target,
                    archivedSnackbarMessage = archivedSnackbarMessage,
                    sessionGateway = sessionGateway,
                    scope = scope,
                    activeSessionId = activeSessionId,
                    onActiveSessionChange = onActiveSessionChange,
                    onShowArchiveUndo = onShowArchiveUndo,
                )
            },
            onDelete = controller::startDelete,
        )
    }
    controller.renameTarget?.let { target ->
        RenameSessionDialog(
            currentTitle = target.title,
            onDismiss = controller::dismissRename,
            onConfirm = { nextTitle ->
                controller.dismissRename()
                scope.launch { sessionGateway.renameSession(target.id, nextTitle) }
            },
        )
    }
    controller.deleteTarget?.let { target ->
        DeleteSessionDialog(
            summary = target,
            onDismiss = controller::dismissDelete,
            onConfirm = {
                controller.dismissDelete()
                if (activeSessionId == target.id) onActiveSessionChange(null)
                scope.launch { sessionGateway.deleteSession(target.id) }
            },
        )
    }
}

private fun toggleArchiveWithUndo(
    target: AgentSessionSummary,
    archivedSnackbarMessage: String,
    sessionGateway: SessionGateway,
    scope: CoroutineScope,
    activeSessionId: String?,
    onActiveSessionChange: (String?) -> Unit,
    onShowArchiveUndo: (String, () -> Unit) -> Unit,
) {
    val archiveNext = !target.isArchived
    val wasActive = activeSessionId == target.id
    if (archiveNext && wasActive) {
        onActiveSessionChange(null)
    }
    scope.launch {
        sessionGateway.setSessionArchived(target.id, archived = archiveNext)
        if (archiveNext) {
            onShowArchiveUndo(archivedSnackbarMessage) {
                scope.launch {
                    sessionGateway.setSessionArchived(target.id, archived = false)
                    if (wasActive) onActiveSessionChange(target.id)
                }
            }
        }
    }
}

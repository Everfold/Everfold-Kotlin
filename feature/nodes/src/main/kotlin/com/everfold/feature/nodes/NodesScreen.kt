package com.everfold.feature.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.ui.connection.labelRes
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toImmutableList
import org.koin.androidx.compose.koinViewModel
import com.everfold.core.ui.R as CoreUiR

@Composable
fun NodesScreen(
    onOpenDrawer: () -> Unit,
    onCreateNode: () -> Unit,
    onEditNode: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NodesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuProfile by remember { mutableStateOf<ConnectionProfile?>(null) }
    var noteTarget by remember { mutableStateOf<ConnectionProfile?>(null) }
    var deleteTarget by remember { mutableStateOf<ConnectionProfile?>(null) }

    NodesScaffold(
        state = state,
        onOpenDrawer = onOpenDrawer,
        onCreateNode = onCreateNode,
        onDisconnectAll = viewModel::disconnectAll,
        onDisconnectProfile = viewModel::disconnectProfile,
        onConnectLocal = viewModel::connectLocal,
        onConnectProfile = viewModel::connectProfile,
        onOpenMenu = { profile -> menuProfile = profile },
        modifier = modifier,
    )

    NodesDialogsHost(
        menuProfile = menuProfile,
        noteTarget = noteTarget,
        deleteTarget = deleteTarget,
        connectedProfileIds = state.connectedProfileIds,
        onDismissMenu = { menuProfile = null },
        onEditConfig = { profile ->
            menuProfile = null
            onEditNode(profile.id)
        },
        onOpenNote = { profile ->
            menuProfile = null
            noteTarget = profile
        },
        onOpenDelete = { profile ->
            menuProfile = null
            deleteTarget = profile
        },
        onDismissNote = { noteTarget = null },
        onSaveNote = { profile, note ->
            noteTarget = null
            viewModel.updateProfileNote(profile.id, note)
        },
        onDismissDelete = { deleteTarget = null },
        onConfirmDelete = { profile ->
            deleteTarget = null
            viewModel.deleteProfile(profile.id)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodesScaffold(
    state: NodesUiState,
    onOpenDrawer: () -> Unit,
    onCreateNode: () -> Unit,
    onDisconnectAll: () -> Unit,
    onDisconnectProfile: (String) -> Unit,
    onConnectLocal: () -> Unit,
    onConnectProfile: (String) -> Unit,
    onOpenMenu: (ConnectionProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(CoreUiR.string.common_open_menu),
                        )
                    }
                },
                title = { Text(stringResource(R.string.nodes_title)) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNode) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.nodes_add_action),
                )
            }
        },
    ) { innerPadding ->
        NodesListContent(
            state = state,
            onDisconnectAll = onDisconnectAll,
            onDisconnectProfile = onDisconnectProfile,
            onConnectLocal = onConnectLocal,
            onConnectProfile = onConnectProfile,
            onOpenMenu = onOpenMenu,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun NodesListContent(
    state: NodesUiState,
    onDisconnectAll: () -> Unit,
    onDisconnectProfile: (String) -> Unit,
    onConnectLocal: () -> Unit,
    onConnectProfile: (String) -> Unit,
    onOpenMenu: (ConnectionProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val connectedProfiles =
        state.savedProfiles.filter { it.id in state.connectedProfileIds }.toImmutableList()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "summary") {
            ConnectionSummaryCard(
                isLocalConnected = state.isLocalConnected,
                connectedProfiles = connectedProfiles,
                onDisconnectAll = onDisconnectAll,
                onConnectLocal = onConnectLocal,
            )
        }
        state.lastErrorCode?.let { errorCode ->
            item(key = "error") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = stringResource(errorCode.labelRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
        item(key = "profiles-header") {
            EverfoldSectionHeader(title = stringResource(R.string.nodes_profiles_header, state.savedProfiles.size))
        }
        if (state.savedProfiles.isEmpty()) {
            item(key = "profiles-empty") {
                EverfoldEmptyState(
                    title = stringResource(R.string.nodes_profiles_empty_title),
                    subtitle = stringResource(R.string.nodes_profiles_empty_subtitle),
                )
            }
        } else {
            items(state.savedProfiles, key = { it.id }) { profile ->
                SavedProfileCard(
                    profile = profile,
                    isActive = profile.id in state.connectedProfileIds,
                    isConnecting = profile.id in state.connectingProfileIds,
                    onConnect = { onConnectProfile(profile.id) },
                    onDisconnect = { onDisconnectProfile(profile.id) },
                    onOpenMenu = { onOpenMenu(profile) },
                )
            }
        }
    }
}

@Composable
private fun NodesDialogsHost(
    menuProfile: ConnectionProfile?,
    noteTarget: ConnectionProfile?,
    deleteTarget: ConnectionProfile?,
    connectedProfileIds: ImmutableSet<String>,
    onDismissMenu: () -> Unit,
    onEditConfig: (ConnectionProfile) -> Unit,
    onOpenNote: (ConnectionProfile) -> Unit,
    onOpenDelete: (ConnectionProfile) -> Unit,
    onDismissNote: () -> Unit,
    onSaveNote: (ConnectionProfile, String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (ConnectionProfile) -> Unit,
) {
    menuProfile?.let { profile ->
        NodeProfileActionSheet(
            profile = profile,
            onDismiss = onDismissMenu,
            onEditConfig = { onEditConfig(profile) },
            onEditNote = { onOpenNote(profile) },
            onDelete = { onOpenDelete(profile) },
        )
    }
    noteTarget?.let { profile ->
        EditNodeNoteDialog(
            profile = profile,
            onDismiss = onDismissNote,
            onConfirm = { note -> onSaveNote(profile, note) },
        )
    }
    deleteTarget?.let { profile ->
        DeleteNodeProfileDialog(
            profile = profile,
            isActive = profile.id in connectedProfileIds,
            onDismiss = onDismissDelete,
            onConfirm = { onConfirmDelete(profile) },
        )
    }
}

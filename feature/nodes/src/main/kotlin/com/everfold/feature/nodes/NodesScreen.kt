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
import androidx.compose.material.icons.rounded.Refresh
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
import com.everfold.core.model.ConnectionState
import com.everfold.core.ui.connection.labelRes
import org.koin.androidx.compose.koinViewModel
import com.everfold.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
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
    val activeProfileId = (state.connectionState as? ConnectionState.Connected)?.endpoint?.profileId

    NodesScaffold(
        state = state,
        activeProfileId = activeProfileId,
        onOpenDrawer = onOpenDrawer,
        onCreateNode = onCreateNode,
        onRefresh = viewModel::refresh,
        onDisconnect = viewModel::disconnect,
        onConnectLocal = viewModel::connectLocal,
        onConnectProfile = viewModel::connectProfile,
        onOpenMenu = { profile -> menuProfile = profile },
        modifier = modifier,
    )

    NodesDialogsHost(
        menuProfile = menuProfile,
        noteTarget = noteTarget,
        deleteTarget = deleteTarget,
        activeProfileId = activeProfileId,
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

@Composable
private fun NodesScaffold(
    state: NodesUiState,
    activeProfileId: String?,
    onOpenDrawer: () -> Unit,
    onCreateNode: () -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onConnectLocal: () -> Unit,
    onConnectProfile: (String) -> Unit,
    onOpenMenu: (ConnectionProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { NodesTopBar(onOpenDrawer = onOpenDrawer, onRefresh = onRefresh) },
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
            activeProfileId = activeProfileId,
            onDisconnect = onDisconnect,
            onConnectLocal = onConnectLocal,
            onConnectProfile = onConnectProfile,
            onOpenMenu = onOpenMenu,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NodesTopBar(
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
) {
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
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.nodes_refresh),
                )
            }
        },
    )
}

@Composable
private fun NodesListContent(
    state: NodesUiState,
    activeProfileId: String?,
    onDisconnect: () -> Unit,
    onConnectLocal: () -> Unit,
    onConnectProfile: (String) -> Unit,
    onOpenMenu: (ConnectionProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "summary") {
            ConnectionSummaryCard(
                connectionState = state.connectionState,
                onDisconnect = onDisconnect,
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
                    isActive = activeProfileId == profile.id,
                    isConnecting = state.connectingProfileId == profile.id,
                    onConnect = { onConnectProfile(profile.id) },
                    onDisconnect = onDisconnect,
                    onOpenMenu = { onOpenMenu(profile) },
                )
            }
        }
        if (state.nodes.isNotEmpty()) {
            item(key = "topology-header") {
                EverfoldSectionHeader(title = stringResource(R.string.nodes_topology_header, state.nodes.size))
            }
            items(state.nodes, key = { it.id }) { node -> NodeTopologyCard(node = node) }
        }
    }
}

@Composable
private fun NodesDialogsHost(
    menuProfile: ConnectionProfile?,
    noteTarget: ConnectionProfile?,
    deleteTarget: ConnectionProfile?,
    activeProfileId: String?,
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
            isActive = activeProfileId == profile.id,
            onDismiss = onDismissDelete,
            onConfirm = { onConfirmDelete(profile) },
        )
    }
}

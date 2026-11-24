package com.everfold.feature.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.ui.connection.RemoteConnectionForm
import com.everfold.core.ui.connection.labelRes
import org.koin.androidx.compose.koinViewModel
import com.everfold.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeEditorScreen(
    profileId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NodeEditorViewModel = koinViewModel(),
) {
    LaunchedEffect(profileId) {
        viewModel.bindProfile(profileId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissIme: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    val errorText = state.errorCode?.let { stringResource(it.labelRes()) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            NodeEditorTopBar(
                isCreate = state.profileId == null,
                isConnecting = state.isConnecting,
                scrollBehavior = scrollBehavior,
                onBack = {
                    dismissIme()
                    onBack()
                },
            )
        },
        bottomBar = {
            NodeEditorBottomBar(
                isConnecting = state.isConnecting,
                onSaveOnly = {
                    dismissIme()
                    viewModel.saveOnly(onBack)
                },
                onSaveAndConnect = {
                    dismissIme()
                    viewModel.saveAndConnect(onBack)
                },
            )
        },
    ) { innerPadding ->
        NodeEditorBody(
            state = state,
            errorText = errorText,
            onTransportChange = viewModel::updateTransport,
            onProfileNameChange = viewModel::updateName,
            onProfileNoteChange = viewModel::updateNote,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun NodeEditorBody(
    state: NodeEditorUiState,
    errorText: String?,
    onTransportChange: (RemoteTransportConfig) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onProfileNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        RemoteConnectionForm(
            transport = state.transport,
            onTransportChange = onTransportChange,
            enabled = !state.isConnecting,
            showProfileFields = true,
            profileName = state.name,
            profileNote = state.note,
            onProfileNameChange = onProfileNameChange,
            onProfileNoteChange = onProfileNoteChange,
            errorText = errorText,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NodeEditorTopBar(
    isCreate: Boolean,
    isConnecting: Boolean,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
) {
    MediumFlexibleTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack, enabled = !isConnecting, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(CoreUiR.string.common_back),
                )
            }
        },
        title = {
            Text(
                stringResource(
                    if (isCreate) R.string.node_editor_create_title else R.string.node_editor_edit_title,
                ),
            )
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NodeEditorBottomBar(
    isConnecting: Boolean,
    onSaveOnly: () -> Unit,
    onSaveAndConnect: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onSaveOnly,
                shapes = ButtonDefaults.shapes(),
                enabled = !isConnecting,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.node_editor_save_only))
            }
            Button(
                onClick = onSaveAndConnect,
                shapes = ButtonDefaults.shapes(),
                enabled = !isConnecting,
                modifier = Modifier.weight(1f),
            ) {
                if (isConnecting) {
                    LoadingIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.node_editor_save_and_connect))
                }
            }
        }
    }
}

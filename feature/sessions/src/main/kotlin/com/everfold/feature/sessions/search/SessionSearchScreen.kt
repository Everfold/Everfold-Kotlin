package com.everfold.feature.sessions.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.clearFocusOnTouch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SessionSearchScreen(
    onBack: () -> Unit,
    onSelectSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionSearchViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissIme: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SessionSearchTopBar(
                    query = state.query,
                    focusRequester = focusRequester,
                    onQueryChange = viewModel::updateQuery,
                    onClearQuery = viewModel::clearQuery,
                    onBack = {
                        dismissIme()
                        onBack()
                    },
                    onSearchAction = dismissIme,
                )
                SessionSearchFilterRow(
                    filter = state.filter,
                    availableWorkspaces = state.availableWorkspaces,
                    availableNodes = state.availableNodes,
                    onDismissIme = dismissIme,
                    onSelectWorkspace = viewModel::selectWorkspace,
                    onSelectNode = viewModel::selectNode,
                    onSelectLoopState = viewModel::selectLoopState,
                    onClearFilters = viewModel::clearFilters,
                )
            }
        },
    ) { innerPadding ->
        SessionSearchBody(
            state = state,
            onSelectSession = { sessionId ->
                dismissIme()
                onSelectSession(sessionId)
            },
            onResetSearch = {
                dismissIme()
                viewModel.clearQuery()
                viewModel.clearFilters()
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding).clearFocusOnTouch(dismissIme),
        )
    }
}

@Composable
private fun SessionSearchBody(
    state: SessionSearchUiState,
    onSelectSession: (String) -> Unit,
    onResetSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCriteria = state.query.isNotBlank() || state.filter.hasActiveFilters
    if (state.hits.isEmpty()) {
        SessionSearchEmptyBody(
            hasCriteria = hasCriteria,
            onResetSearch = onResetSearch,
            modifier = modifier,
        )
    } else {
        val headerText =
            if (hasCriteria) {
                pluralStringResource(
                    id = R.plurals.session_search_result_count,
                    count = state.hits.size,
                    state.hits.size,
                )
            } else {
                stringResource(R.string.session_search_recent_header)
            }
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") {
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            items(state.hits, key = { it.summary.id }) { hit ->
                SessionSearchHitCard(
                    hit = hit,
                    query = state.query,
                    onClick = { onSelectSession(hit.summary.id) },
                )
            }
        }
    }
}

@Composable
private fun SessionSearchEmptyBody(
    hasCriteria: Boolean,
    onResetSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EverfoldEmptyState(
                title = stringResource(R.string.session_search_empty_title),
                subtitle = stringResource(R.string.session_search_empty_subtitle),
            )
            if (hasCriteria) {
                FilledTonalButton(onClick = onResetSearch) {
                    Text(stringResource(R.string.session_search_reset))
                }
            }
        }
    }
}

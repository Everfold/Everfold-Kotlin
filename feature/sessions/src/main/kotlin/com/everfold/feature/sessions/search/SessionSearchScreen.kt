package com.everfold.feature.sessions.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.effectsSpec
import com.everfold.core.designsystem.theme.effectsSpecFor
import com.everfold.core.designsystem.theme.spatialSpec
import com.everfold.core.designsystem.theme.spatialSpecFor
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.LoopState
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.clearFocusOnTouch
import com.everfold.feature.sessions.component.SyncedBasicTextField
import org.koin.androidx.compose.koinViewModel
import com.everfold.core.ui.R as CoreUiR

private const val SEARCH_BODY_INITIAL_SCALE = 0.98f
private const val SEARCH_BAR_HEIGHT_DP = 52
private const val CLEAR_BTN_INITIAL_SCALE = 0.8f

private class SearchHeaderActions(
    val onQueryChange: (String) -> Unit,
    val onClearQuery: () -> Unit,
    val onBack: () -> Unit,
    val onSearchAction: () -> Unit,
    val onSelectWorkspace: (String?) -> Unit,
    val onSelectNode: (String?) -> Unit,
    val onSelectLoopState: (LoopState?) -> Unit,
    val onToggleArchiveFilter: () -> Unit,
    val onClearFilters: () -> Unit,
)

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
    val dismissIme: () -> Unit = { focusManager.clearFocus(force = true).also { keyboardController?.hide() } }
    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.canScrollBackward } }
    val headerActions =
        SearchHeaderActions(
            onQueryChange = viewModel::updateQuery,
            onClearQuery = viewModel::clearQuery,
            onBack = { dismissIme().also { onBack() } },
            onSearchAction = dismissIme,
            onSelectWorkspace = viewModel::selectWorkspace,
            onSelectNode = viewModel::selectNode,
            onSelectLoopState = viewModel::selectLoopState,
            onToggleArchiveFilter = viewModel::toggleIncludeArchived,
            onClearFilters = viewModel::clearFilters,
        )

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            SessionSearchHeader(
                state = state,
                focusRequester = focusRequester,
                isScrolled = isScrolled,
                actions = headerActions,
            )
        },
    ) { innerPadding ->
        SessionSearchBody(
            state = state,
            listState = listState,
            bottomPadding = innerPadding.calculateBottomPadding(),
            onSelectSession = { sessionId -> dismissIme().also { onSelectSession(sessionId) } },
            onResetSearch = {
                dismissIme()
                viewModel.clearQuery()
                viewModel.clearFilters()
            },
            modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()).clearFocusOnTouch(dismissIme),
        )
    }
}

@Composable
private fun SessionSearchHeader(
    state: SessionSearchUiState,
    focusRequester: FocusRequester,
    isScrolled: Boolean,
    actions: SearchHeaderActions,
) {
    val headerColor by
        animateColorAsState(
            targetValue =
                if (isScrolled) {
                    MaterialTheme.colorScheme.surfaceContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            animationSpec = LocalAnimationSpeed.current.effectsSpec(),
            label = "search_header_color",
        )
    Surface(modifier = Modifier.fillMaxWidth(), color = headerColor) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SessionSearchTopBar(
                query = state.query,
                focusRequester = focusRequester,
                onQueryChange = actions.onQueryChange,
                onClearQuery = actions.onClearQuery,
                onBack = actions.onBack,
                onSearchAction = actions.onSearchAction,
            )
            SessionSearchFilterRow(
                filter = state.filter,
                availableWorkspaces = state.availableWorkspaces,
                availableNodes = state.availableNodes,
                onDismissIme = actions.onSearchAction,
                onSelectWorkspace = actions.onSelectWorkspace,
                onSelectNode = actions.onSelectNode,
                onSelectLoopState = actions.onSelectLoopState,
                onToggleArchiveFilter = actions.onToggleArchiveFilter,
                onClearFilters = actions.onClearFilters,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionSearchTopBar(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBack: () -> Unit,
    onSearchAction: () -> Unit,
) {
    val animationSpeed = LocalAnimationSpeed.current
    val effectsSpec = animationSpeed.effectsSpec<Float>()
    val spatialSpec = animationSpeed.spatialSpec<Float>()
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                .height(SEARCH_BAR_HEIGHT_DP.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(CoreUiR.string.common_back),
                )
            }
            SyncedBasicTextField(
                value = query,
                placeholder = stringResource(R.string.session_search_hint),
                onValueChange = onQueryChange,
                lineLimits = TextFieldLineLimits.SingleLine,
                placeholderMaxLines = 1,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { onSearchAction() },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp).focusRequester(focusRequester),
            )
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(effectsSpec) + scaleIn(spatialSpec, initialScale = CLEAR_BTN_INITIAL_SCALE),
                exit = fadeOut(effectsSpec) + scaleOut(spatialSpec, targetScale = CLEAR_BTN_INITIAL_SCALE),
            ) {
                IconButton(onClick = onClearQuery, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.session_search_clear_query),
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionSearchBody(
    state: SessionSearchUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    onSelectSession: (String) -> Unit,
    onResetSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasCriteria = state.query.isNotBlank() || state.filter.hasActiveFilters
    val animationSpeed = LocalAnimationSpeed.current
    AnimatedContent(
        targetState = state.hits.isEmpty(),
        transitionSpec = { searchBodyTransition(animationSpeed) },
        modifier = modifier,
        label = "SearchBody",
    ) { isEmpty ->
        if (isEmpty) {
            SessionSearchEmptyBody(
                hasCriteria = hasCriteria,
                onResetSearch = onResetSearch,
                modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding),
            )
        } else {
            SessionSearchHitsList(
                state = state,
                hasCriteria = hasCriteria,
                listState = listState,
                bottomPadding = bottomPadding,
                onSelectSession = onSelectSession,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionSearchHitsList(
    state: SessionSearchUiState,
    hasCriteria: Boolean,
    listState: LazyListState,
    bottomPadding: Dp,
    onSelectSession: (String) -> Unit,
) {
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
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + bottomPadding),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        item(key = "header") {
            EverfoldSectionHeader(
                title = headerText,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        itemsIndexed(state.hits, key = { _, hit -> hit.summary.id }) { index, hit ->
            SessionSearchHitCard(
                index = index,
                count = state.hits.size,
                hit = hit,
                query = state.query,
                onClick = { onSelectSession(hit.summary.id) },
            )
        }
    }
}

private fun searchBodyTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val effectsSpec = speed.effectsSpecFor<Float>()
        val spatialSpec = speed.spatialSpecFor<Float>()
        (fadeIn(effectsSpec) + scaleIn(spatialSpec, initialScale = SEARCH_BODY_INITIAL_SCALE)) togetherWith
            fadeOut(effectsSpec)
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                icon = Icons.Rounded.Search,
            )
            if (hasCriteria) {
                FilledTonalButton(onClick = onResetSearch, shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.session_search_reset))
                }
            }
        }
    }
}

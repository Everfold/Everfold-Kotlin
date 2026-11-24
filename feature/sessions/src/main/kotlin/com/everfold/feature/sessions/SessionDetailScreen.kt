package com.everfold.feature.sessions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldLogo
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.scaleDuration
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.LoopState
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.WorkspaceDescriptor
import com.everfold.feature.sessions.component.ArchivedSessionBottomBar
import com.everfold.feature.sessions.component.ObservedItemRow
import com.everfold.feature.sessions.component.SessionComposerBar
import com.everfold.feature.sessions.component.SessionContextDetailSheet
import com.everfold.feature.sessions.component.SessionContextPickerSheet
import com.everfold.feature.sessions.component.SessionDetailTopBar
import com.everfold.feature.sessions.component.SessionModelEffortSheet
import org.koin.androidx.compose.koinViewModel

private const val EMPTY_LOGO_ALPHA = 0.75f
private const val SESSION_BODY_OUT_MS = 80
private const val SESSION_BODY_IN_MS = 200
private const val SESSION_BODY_INITIAL_SCALE = 0.96f

@Composable
fun SessionDetailScreen(
    sessionId: String?,
    onOpenDrawer: () -> Unit,
    onSessionCreate: (String) -> Unit,
    onNewSession: () -> Unit,
    onOpenSessionMenu: (AgentSessionSummary) -> Unit,
    onOpenNodes: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(sessionId) { viewModel.bindSession(sessionId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SessionDetailScreenContent(
        state = state,
        onOpenDrawer = onOpenDrawer,
        onNewSession = onNewSession,
        onOpenSessionMenu = onOpenSessionMenu,
        onOpenNodes = onOpenNodes,
        onRestore = viewModel::restoreActiveSession,
        onDraftChange = viewModel::updateDraft,
        onSend = { viewModel.sendDraft(onSessionCreate) },
        onCancel = viewModel::cancelActiveLoop,
        onSelectContext = viewModel::selectRuntimeContext,
        onSelectModelAndEffort = viewModel::selectModelAndEffort,
        modifier = modifier,
    )
}

@Composable
private fun SessionDetailScreenContent(
    state: SessionDetailUiState,
    onOpenDrawer: () -> Unit,
    onNewSession: () -> Unit,
    onOpenSessionMenu: (AgentSessionSummary) -> Unit,
    onOpenNodes: () -> Unit,
    onRestore: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onSelectContext: (NodeDescriptor, WorkspaceDescriptor?) -> Unit,
    onSelectModelAndEffort: (String, ReasoningEffort?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    val isRunning = detail?.summary?.loopState == LoopState.PLANNING || detail?.summary?.loopState == LoopState.EXECUTING_ACTION
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissIme: () -> Unit = { focusManager.clearFocus(force = true).also { keyboardController?.hide() } }
    var showContextPicker by rememberSaveable { mutableStateOf(false) }
    var showContextDetail by rememberSaveable { mutableStateOf(false) }
    var showModelSheet by rememberSaveable { mutableStateOf(false) }
    val handleContextClick: () -> Unit = {
        dismissIme()
        if (detail != null) {
            showContextDetail = true
        } else if (state.hasConnectedRelay) {
            showContextPicker = true
        } else {
            onOpenNodes()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            topBar = {
                SessionDetailTopBar(
                    state = state,
                    onOpenDrawer = { dismissIme().also { onOpenDrawer() } },
                    onContextClick = handleContextClick,
                    onNewSession = { dismissIme().also { onNewSession() } },
                    onOpenMenu = { summary -> dismissIme().also { onOpenSessionMenu(summary) } },
                )
            },
            bottomBar = {
                SessionDetailBottomBar(
                    isArchived = detail?.summary?.isArchived == true,
                    draft = state.draft,
                    isRunning = isRunning,
                    onRestore = onRestore,
                    onDraftChange = onDraftChange,
                    onSend = { dismissIme().also { onSend() } },
                    onCancel = { dismissIme().also { onCancel() } },
                )
            },
        ) { innerPadding ->
            SessionDetailBody(state = state, modifier = Modifier.fillMaxSize().padding(innerPadding).clearFocusOnTouch(dismissIme))
        }
        SessionDetailSheets(
            state = state,
            showContextPicker = showContextPicker,
            showContextDetail = showContextDetail,
            showModelSheet = showModelSheet,
            onSelectContext = onSelectContext,
            onSelectModelAndEffort = onSelectModelAndEffort,
            onOpenNodes = onOpenNodes,
            onDismissPicker = { showContextPicker = false },
            onDismissDetail = { showContextDetail = false },
            onDismissModel = { showModelSheet = false },
        )
    }
}

@Composable
private fun SessionDetailSheets(
    state: SessionDetailUiState,
    showContextPicker: Boolean,
    showContextDetail: Boolean,
    showModelSheet: Boolean,
    onSelectContext: (NodeDescriptor, WorkspaceDescriptor?) -> Unit,
    onSelectModelAndEffort: (String, ReasoningEffort?) -> Unit,
    onOpenNodes: () -> Unit,
    onDismissPicker: () -> Unit,
    onDismissDetail: () -> Unit,
    onDismissModel: () -> Unit,
) {
    if (showContextPicker && state.detail == null) {
        SessionContextPickerSheet(
            nodes = state.availableNodes,
            selectedContext = state.activeContext,
            onSelect = onSelectContext,
            onOpenNodes = onOpenNodes,
            onDismiss = onDismissPicker,
        )
    }
    val detailContext = state.activeContext
    if (showContextDetail && state.detail != null && detailContext != null) {
        SessionContextDetailSheet(context = detailContext, onDismiss = onDismissDetail)
    }
    if (showModelSheet) {
        SessionModelEffortSheet(
            models = state.availableModels,
            activeModel = state.activeModel,
            activeEffort = state.activeEffort,
            onSelectModelAndEffort = onSelectModelAndEffort,
            onDismiss = onDismissModel,
        )
    }
}

@Composable
private fun SessionDetailBottomBar(
    isArchived: Boolean,
    draft: String,
    isRunning: Boolean,
    onRestore: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    if (isArchived) {
        ArchivedSessionBottomBar(onRestore = onRestore)
    } else {
        SessionComposerBar(
            draft = draft,
            isRunning = isRunning,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun SessionDetailBody(
    state: SessionDetailUiState,
    modifier: Modifier = Modifier,
) {
    val animationSpeed = LocalAnimationSpeed.current
    AnimatedContent(
        targetState = state,
        contentKey = { it.sessionId },
        transitionSpec = { sessionBodyTransition(animationSpeed) },
        modifier = modifier,
        label = "SessionBody",
    ) { bodyState ->
        SessionBodyContent(state = bodyState, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SessionBodyContent(
    state: SessionDetailUiState,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    when {
        state.sessionId == null || (detail != null && detail.items.isEmpty()) -> {
            SessionEmptyPlaceholder(modifier = modifier)
        }

        detail == null -> {
            Box(modifier = modifier)
        }

        else -> {
            SessionMessageList(detail = detail, state = state, modifier = modifier)
        }
    }
}

@Composable
private fun SessionMessageList(
    detail: AgentSessionDetail,
    state: SessionDetailUiState,
    modifier: Modifier = Modifier,
) {
    val currentDensity = LocalDensity.current
    val scaledDensity =
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * state.chatTextSize.scale,
        )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(detail.items, key = { it.id }) { item ->
                ObservedItemRow(
                    item = item,
                    autoCollapseLongMessages = state.autoCollapseLongMessages,
                    expandRunningActionTraces = state.expandRunningActionTraces,
                    expandCompletedActionTraces = state.expandCompletedActionTraces,
                )
            }
        }
    }
}

private fun sessionBodyTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val outMs = speed.scaleDuration(SESSION_BODY_OUT_MS)
        val inMs = speed.scaleDuration(SESSION_BODY_IN_MS)
        val inSpec = tween<Float>(inMs, delayMillis = outMs)
        (fadeIn(inSpec) + scaleIn(inSpec, initialScale = SESSION_BODY_INITIAL_SCALE)) togetherWith
            fadeOut(tween(outMs))
    }

@Composable
private fun SessionEmptyPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EverfoldLogo(modifier = Modifier.size(88.dp).alpha(EMPTY_LOGO_ALPHA))
            Text(
                text = stringResource(R.string.session_detail_empty_quote),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

package com.everfold.feature.sessions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldLogo
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.effectsSpecFor
import com.everfold.core.designsystem.theme.spatialSpecFor
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.LoopState
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionAttachment
import com.everfold.core.model.WorkspaceDescriptor
import com.everfold.feature.sessions.component.ArchivedSessionBottomBar
import com.everfold.feature.sessions.component.SessionComposerBar
import com.everfold.feature.sessions.component.SessionContextDetailSheet
import com.everfold.feature.sessions.component.SessionContextPickerSheet
import com.everfold.feature.sessions.component.SessionDetailTopBar
import com.everfold.feature.sessions.component.SessionFullscreenComposerDialog
import com.everfold.feature.sessions.component.SessionMessageList
import com.everfold.feature.sessions.component.SessionModelEffortSheet
import org.koin.androidx.compose.koinViewModel

private const val EMPTY_LOGO_ALPHA = 0.75f
private const val SESSION_BODY_INITIAL_SCALE = 0.96f

private class ScreenActions(
    val onOpenDrawer: () -> Unit,
    val onNewSession: () -> Unit,
    val onOpenSessionMenu: (AgentSessionSummary) -> Unit,
    val onOpenNodes: () -> Unit,
    val onRestore: () -> Unit,
    val onDraftChange: (String) -> Unit,
    val onAddAttachments: (List<SessionAttachment>) -> Unit,
    val onRemoveAttachment: (String) -> Unit,
    val onSend: () -> Unit,
    val onPause: () -> Unit,
    val onResume: () -> Unit,
    val onCancel: () -> Unit,
    val onSelectContext: (NodeDescriptor, WorkspaceDescriptor?) -> Unit,
    val onSelectModelAndEffort: (String, ReasoningEffort?) -> Unit,
)

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
    val actions =
        ScreenActions(
            onOpenDrawer = onOpenDrawer,
            onNewSession = onNewSession,
            onOpenSessionMenu = onOpenSessionMenu,
            onOpenNodes = onOpenNodes,
            onRestore = viewModel::restoreActiveSession,
            onDraftChange = viewModel::updateDraft,
            onAddAttachments = viewModel::addAttachments,
            onRemoveAttachment = viewModel::removeAttachment,
            onSend = { viewModel.sendDraft(onSessionCreate) },
            onPause = viewModel::pauseActiveLoop,
            onResume = viewModel::resumeActiveLoop,
            onCancel = viewModel::cancelActiveLoop,
            onSelectContext = viewModel::selectRuntimeContext,
            onSelectModelAndEffort = viewModel::selectModelAndEffort,
        )
    SessionDetailScreenContent(state = state, actions = actions, modifier = modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailScreenContent(
    state: SessionDetailUiState,
    actions: ScreenActions,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissIme: () -> Unit = { focusManager.clearFocus(force = true).also { keyboardController?.hide() } }
    var showContextPicker by rememberSaveable { mutableStateOf(false) }
    var showContextDetail by rememberSaveable { mutableStateOf(false) }
    var showModelSheet by rememberSaveable { mutableStateOf(false) }
    var showFullscreenComposer by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val handleContextClick: () -> Unit = {
        dismissIme()
        if (detail != null) {
            showContextDetail = true
        } else if (state.hasConnectedRelay) {
            showContextPicker = true
        } else {
            actions.onOpenNodes()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                SessionDetailTopBar(
                    state = state,
                    scrollBehavior = scrollBehavior,
                    onOpenDrawer = { dismissIme().also { actions.onOpenDrawer() } },
                    onContextClick = handleContextClick,
                    onNewSession = { dismissIme().also { actions.onNewSession() } },
                    onOpenMenu = { summary -> dismissIme().also { actions.onOpenSessionMenu(summary) } },
                )
            },
            bottomBar = {
                SessionDetailBottomBar(
                    state = state,
                    actions = actions,
                    dismissIme = dismissIme,
                    onOpenModelSheet = { showModelSheet = true },
                    onOpenFullscreen = { showFullscreenComposer = true },
                )
            },
        ) { innerPadding ->
            SessionDetailBody(state = state, modifier = Modifier.fillMaxSize().padding(innerPadding).clearFocusOnTouch(dismissIme))
        }
        SessionDetailOverlays(
            state = state,
            actions = actions,
            showContextPicker = showContextPicker,
            showContextDetail = showContextDetail,
            showModelSheet = showModelSheet,
            showFullscreenComposer = showFullscreenComposer,
            onOpenModelSheet = { showModelSheet = true },
            onDismissPicker = { showContextPicker = false },
            onDismissDetail = { showContextDetail = false },
            onDismissModel = { showModelSheet = false },
            onDismissFullscreen = { showFullscreenComposer = false },
        )
    }
}

@Composable
private fun SessionDetailOverlays(
    state: SessionDetailUiState,
    actions: ScreenActions,
    showContextPicker: Boolean,
    showContextDetail: Boolean,
    showModelSheet: Boolean,
    showFullscreenComposer: Boolean,
    onOpenModelSheet: () -> Unit,
    onDismissPicker: () -> Unit,
    onDismissDetail: () -> Unit,
    onDismissModel: () -> Unit,
    onDismissFullscreen: () -> Unit,
) {
    if (showFullscreenComposer && state.detail?.summary?.isArchived != true) {
        SessionFullscreenComposerDialog(
            draft = state.draft,
            attachments = state.draftAttachments,
            loopState = state.detail?.summary?.loopState ?: LoopState.IDLE,
            activeModel = state.activeModel,
            activeEffort = state.activeEffort,
            onDraftChange = actions.onDraftChange,
            onAddAttachments = actions.onAddAttachments,
            onRemoveAttachment = actions.onRemoveAttachment,
            onOpenModelSheet = onOpenModelSheet,
            onSend = actions.onSend,
            onPause = actions.onPause,
            onResume = actions.onResume,
            onCancel = actions.onCancel,
            onDismiss = onDismissFullscreen,
        )
    }
    if (showContextPicker && state.detail == null) {
        SessionContextPickerSheet(
            nodes = state.availableNodes,
            selectedContext = state.activeContext,
            onSelect = actions.onSelectContext,
            onOpenNodes = actions.onOpenNodes,
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
            onSelectModelAndEffort = actions.onSelectModelAndEffort,
            onDismiss = onDismissModel,
        )
    }
}

@Composable
private fun SessionDetailBottomBar(
    state: SessionDetailUiState,
    actions: ScreenActions,
    dismissIme: () -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenFullscreen: () -> Unit,
) {
    if (state.detail?.summary?.isArchived == true) {
        ArchivedSessionBottomBar(onRestore = actions.onRestore)
    } else {
        SessionComposerBar(
            draft = state.draft,
            attachments = state.draftAttachments,
            loopState = state.detail?.summary?.loopState ?: LoopState.IDLE,
            activeModel = state.activeModel,
            activeEffort = state.activeEffort,
            onDraftChange = actions.onDraftChange,
            onAddAttachments = actions.onAddAttachments,
            onRemoveAttachment = actions.onRemoveAttachment,
            onOpenModelSheet = { dismissIme().also { onOpenModelSheet() } },
            onOpenFullscreen = onOpenFullscreen,
            onSend = { dismissIme().also { actions.onSend() } },
            onPause = { dismissIme().also { actions.onPause() } },
            onResume = { dismissIme().also { actions.onResume() } },
            onCancel = { dismissIme().also { actions.onCancel() } },
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

private fun sessionBodyTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val effectsSpec = speed.effectsSpecFor<Float>()
        val spatialSpec = speed.spatialSpecFor<Float>()
        (fadeIn(effectsSpec) + scaleIn(spatialSpec, initialScale = SESSION_BODY_INITIAL_SCALE)) togetherWith
            fadeOut(effectsSpec)
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

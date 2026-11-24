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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldLogo
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.scaleDuration
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.LoopState
import com.everfold.feature.sessions.component.ArchivedSessionBottomBar
import com.everfold.feature.sessions.component.ObservedItemRow
import com.everfold.feature.sessions.component.SessionComposerBar
import org.koin.androidx.compose.koinViewModel
import com.everfold.core.ui.R as CoreUiR

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
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(sessionId) {
        viewModel.bindSession(sessionId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.detail
    val isRunning =
        detail?.summary?.loopState == LoopState.PLANNING ||
            detail?.summary?.loopState == LoopState.EXECUTING_ACTION
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissIme: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            SessionDetailTopBar(
                detail = detail,
                defaultModelName = state.defaultModelName,
                onOpenDrawer = {
                    dismissIme()
                    onOpenDrawer()
                },
                onNewSession = {
                    dismissIme()
                    onNewSession()
                },
                onOpenMenu = { summary ->
                    dismissIme()
                    onOpenSessionMenu(summary)
                },
            )
        },
        bottomBar = {
            SessionDetailBottomBar(
                isArchived = detail?.summary?.isArchived == true,
                draft = state.draft,
                isRunning = isRunning,
                onRestore = viewModel::restoreActiveSession,
                onDraftChange = viewModel::updateDraft,
                onSend = {
                    dismissIme()
                    viewModel.sendDraft(onSessionCreate)
                },
                onCancel = {
                    dismissIme()
                    viewModel.cancelActiveLoop()
                },
            )
        },
    ) { innerPadding ->
        SessionDetailBody(
            state = state,
            modifier = Modifier.fillMaxSize().padding(innerPadding).clearFocusOnTouch(dismissIme),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailTopBar(
    detail: AgentSessionDetail?,
    defaultModelName: String,
    onOpenDrawer: () -> Unit,
    onNewSession: () -> Unit,
    onOpenMenu: (AgentSessionSummary) -> Unit,
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
        title = {
            Column {
                Text(
                    text = detail?.summary?.title ?: stringResource(R.string.session_detail_new_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                val subtitle =
                    if (detail != null) {
                        stringResource(R.string.session_detail_host_label, detail.summary.loopHostNodeName)
                    } else {
                        defaultModelName.ifBlank { stringResource(R.string.session_detail_new_subtitle) }
                    }
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            if (detail != null) {
                if (detail.summary.isArchived) {
                    EverfoldStatusPill(
                        text = stringResource(R.string.session_archived_badge),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    EverfoldStatusPill(text = stringResource(detail.summary.loopState.labelRes()))
                    IconButton(onClick = onNewSession) {
                        Icon(
                            imageVector = Icons.Rounded.AddComment,
                            contentDescription = stringResource(CoreUiR.string.common_new_session),
                        )
                    }
                }
                IconButton(onClick = { onOpenMenu(detail.summary) }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.session_menu_more_actions),
                    )
                }
            }
        },
    )
}

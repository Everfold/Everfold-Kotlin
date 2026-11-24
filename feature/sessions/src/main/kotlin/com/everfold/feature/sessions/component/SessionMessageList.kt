package com.everfold.feature.sessions.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.scaleDuration
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.SessionDetailUiState
import kotlinx.coroutines.launch

private const val BOTTOM_ANCHOR_KEY = "__session_bottom_anchor__"
private const val FAB_ANIM_DURATION_MS = 160
private const val FAB_INITIAL_SCALE = 0.85f

@Composable
internal fun SessionMessageList(
    detail: AgentSessionDetail,
    state: SessionDetailUiState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val animationSpeed = LocalAnimationSpeed.current
    val showScrollToBottom by remember { derivedStateOf { listState.canScrollForward } }
    val currentDensity = LocalDensity.current
    val scaledDensity =
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * state.chatTextSize.scale,
        )
    val isPaused = detail.summary.loopState == LoopState.PAUSED

    SessionMessageScrollEffects(
        detail = detail,
        listState = listState,
        isAtBottom = !showScrollToBottom,
        animationSpeed = animationSpeed,
    )

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(detail.items, key = { it.id }) { item ->
                    ObservedItemRow(
                        item = item,
                        isPaused = isPaused,
                        autoCollapseLongMessages = state.autoCollapseLongMessages,
                        expandRunningActionTraces = state.expandRunningActionTraces,
                        expandCompletedActionTraces = state.expandCompletedActionTraces,
                    )
                }
                item(key = BOTTOM_ANCHOR_KEY) {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
        ScrollToLatestButton(
            visible = showScrollToBottom,
            animationSpeed = animationSpeed,
            onClick = {
                scope.launch {
                    val bottomIndex = detail.items.size
                    if (animationSpeed == AnimationSpeed.OFF) {
                        listState.scrollToItem(bottomIndex)
                    } else {
                        listState.animateScrollToItem(bottomIndex)
                    }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun SessionMessageScrollEffects(
    detail: AgentSessionDetail,
    listState: LazyListState,
    isAtBottom: Boolean,
    animationSpeed: AnimationSpeed,
) {
    var wasAtBottom by remember { mutableStateOf(true) }
    val bottomIndex = detail.items.size
    val lastItem = detail.items.lastOrNull()
    val streamingLength = (lastItem as? ObservedItem.AssistantMessage)?.takeIf { it.isStreaming }?.markdown?.length

    LaunchedEffect(isAtBottom) {
        wasAtBottom = isAtBottom
    }
    LaunchedEffect(detail.summary.id) {
        if (detail.items.isNotEmpty()) {
            listState.scrollToItem(bottomIndex)
        }
    }
    LaunchedEffect(detail.items.size, lastItem?.id) {
        if (detail.items.isNotEmpty() && (lastItem is ObservedItem.UserMessage || wasAtBottom)) {
            if (animationSpeed == AnimationSpeed.OFF) {
                listState.scrollToItem(bottomIndex)
            } else {
                listState.animateScrollToItem(bottomIndex)
            }
        }
    }
    LaunchedEffect(streamingLength) {
        if (streamingLength != null && wasAtBottom && detail.items.isNotEmpty()) {
            listState.scrollToItem(bottomIndex)
        }
    }
}

@Composable
private fun ScrollToLatestButton(
    visible: Boolean,
    animationSpeed: AnimationSpeed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animMs = animationSpeed.scaleDuration(FAB_ANIM_DURATION_MS)
    val enter =
        if (animationSpeed == AnimationSpeed.OFF) {
            EnterTransition.None
        } else {
            fadeIn(tween(animMs)) + scaleIn(tween(animMs), initialScale = FAB_INITIAL_SCALE)
        }
    val exit =
        if (animationSpeed == AnimationSpeed.OFF) {
            ExitTransition.None
        } else {
            fadeOut(tween(animMs)) + scaleOut(tween(animMs), targetScale = FAB_INITIAL_SCALE)
        }
    AnimatedVisibility(visible = visible, enter = enter, exit = exit, modifier = modifier) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.session_detail_scroll_to_latest),
            )
        }
    }
}

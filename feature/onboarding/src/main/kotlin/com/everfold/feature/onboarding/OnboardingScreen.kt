package com.everfold.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.effectsSpecFor
import com.everfold.core.designsystem.theme.spatialSpecFor
import com.everfold.core.designsystem.theme.toMotionDurationScale
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import com.everfold.feature.onboarding.step.OnboardingModeStep
import com.everfold.feature.onboarding.step.OnboardingPermissionsStep
import com.everfold.feature.onboarding.step.OnboardingQuickSettingsStep
import com.everfold.feature.onboarding.step.OnboardingRemoteStep
import com.everfold.feature.onboarding.step.OnboardingShowcaseStep
import com.everfold.feature.onboarding.step.SHOWCASE_PAGE_COUNT
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import com.everfold.core.ui.R as CoreUiR

private const val STEP_SLIDE_DIVISOR = 10

private data class OnboardingActions(
    val onBack: () -> Unit,
    val onSkip: () -> Unit,
    val onPrimaryClick: () -> Unit,
    val onLanguageChange: (AppLanguage) -> Unit,
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
    val onSelectMode: (ConnectionMode) -> Unit,
    val onProfileNameChange: (String) -> Unit,
    val onProfileNoteChange: (String) -> Unit,
    val onTransportChange: (RemoteTransportConfig) -> Unit,
)

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { SHOWCASE_PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val animationSpeed = LocalAnimationSpeed.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val dismissIme: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    BackHandler(enabled = state.step != OnboardingStep.SHOWCASE && !state.isConnecting) {
        dismissIme()
        viewModel.previousStep()
    }

    val actions =
        OnboardingActions(
            onBack = {
                dismissIme()
                viewModel.previousStep()
            },
            onSkip = {
                dismissIme()
                if (state.step == OnboardingStep.SHOWCASE) viewModel.skipShowcase() else viewModel.nextStep()
            },
            onPrimaryClick = {
                dismissIme()
                if (state.step == OnboardingStep.SHOWCASE && pagerState.currentPage < SHOWCASE_PAGE_COUNT - 1) {
                    scope.launch(animationSpeed.toMotionDurationScale()) {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    viewModel.nextStep()
                }
            },
            onLanguageChange = viewModel::updateLanguage,
            onThemeModeChange = viewModel::updateThemeMode,
            onColorChange = viewModel::selectColor,
            onSelectMode = { mode ->
                dismissIme()
                viewModel.selectMode(mode)
            },
            onProfileNameChange = viewModel::updateProfileName,
            onProfileNoteChange = viewModel::updateProfileNote,
            onTransportChange = viewModel::updateTransport,
        )

    OnboardingScaffold(
        state = state,
        pagerState = pagerState,
        animationSpeed = animationSpeed,
        actions = actions,
        modifier = modifier,
    )
}

@Composable
private fun OnboardingScaffold(
    state: OnboardingUiState,
    pagerState: PagerState,
    animationSpeed: AnimationSpeed,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            OnboardingTopBar(
                step = state.step,
                isConnecting = state.isConnecting,
                onBack = actions.onBack,
                onSkip = actions.onSkip,
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                state = state,
                showcasePage = pagerState.currentPage,
                onPrimaryClick = actions.onPrimaryClick,
            )
        },
    ) { innerPadding ->
        OnboardingStepContent(
            state = state,
            pagerState = pagerState,
            animationSpeed = animationSpeed,
            onLanguageChange = actions.onLanguageChange,
            onThemeModeChange = actions.onThemeModeChange,
            onColorChange = actions.onColorChange,
            onSelectMode = actions.onSelectMode,
            onProfileNameChange = actions.onProfileNameChange,
            onProfileNoteChange = actions.onProfileNoteChange,
            onTransportChange = actions.onTransportChange,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnboardingTopBar(
    step: OnboardingStep,
    isConnecting: Boolean,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    val progress = (step.ordinal + 1).toFloat() / OnboardingStep.entries.size.toFloat()
    val canSkip = step == OnboardingStep.SHOWCASE || step == OnboardingStep.PERMISSIONS
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (step != OnboardingStep.SHOWCASE) {
            IconButton(
                onClick = onBack,
                shapes = IconButtonDefaults.shapes(),
                enabled = !isConnecting,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(CoreUiR.string.common_back),
                )
            }
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
        LinearWavyProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f),
        )
        if (canSkip) {
            TextButton(
                onClick = onSkip,
                shapes = ButtonDefaults.shapes(),
                enabled = !isConnecting,
            ) {
                Text(stringResource(R.string.onboarding_skip))
            }
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun OnboardingStepContent(
    state: OnboardingUiState,
    pagerState: PagerState,
    animationSpeed: AnimationSpeed,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
    onSelectMode: (ConnectionMode) -> Unit,
    onProfileNameChange: (String) -> Unit,
    onProfileNoteChange: (String) -> Unit,
    onTransportChange: (RemoteTransportConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            stepSlideTransition(
                forward = targetState.ordinal >= initialState.ordinal,
                speed = animationSpeed,
            )
        },
        modifier = modifier,
        label = "OnboardingStep",
    ) { step ->
        when (step) {
            OnboardingStep.SHOWCASE -> {
                OnboardingShowcaseStep(pagerState = pagerState)
            }

            OnboardingStep.BASIC_SETTINGS -> {
                OnboardingQuickSettingsStep(
                    preferences = state.preferences,
                    onLanguageChange = onLanguageChange,
                    onThemeModeChange = onThemeModeChange,
                    onColorChange = onColorChange,
                )
            }

            OnboardingStep.PERMISSIONS -> {
                OnboardingPermissionsStep()
            }

            OnboardingStep.CONNECTION_MODE -> {
                OnboardingModeStep(
                    selectedMode = state.selectedMode,
                    onSelectMode = onSelectMode,
                )
            }

            OnboardingStep.REMOTE_CONFIG -> {
                OnboardingRemoteStep(
                    profileName = state.profileName,
                    profileNote = state.profileNote,
                    transport = state.transport,
                    enabled = !state.isConnecting,
                    errorCode = state.errorCode,
                    onProfileNameChange = onProfileNameChange,
                    onProfileNoteChange = onProfileNoteChange,
                    onTransportChange = onTransportChange,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnboardingBottomBar(
    state: OnboardingUiState,
    showcasePage: Int,
    onPrimaryClick: () -> Unit,
) {
    val labelRes =
        when (state.step) {
            OnboardingStep.SHOWCASE -> {
                if (showcasePage < SHOWCASE_PAGE_COUNT - 1) {
                    R.string.onboarding_cta_next
                } else {
                    R.string.onboarding_cta_start
                }
            }

            OnboardingStep.BASIC_SETTINGS, OnboardingStep.PERMISSIONS -> {
                R.string.onboarding_cta_continue
            }

            OnboardingStep.CONNECTION_MODE -> {
                if (state.selectedMode == ConnectionMode.LOCAL_ONLY) {
                    R.string.onboarding_cta_local
                } else {
                    R.string.onboarding_cta_configure_remote
                }
            }

            OnboardingStep.REMOTE_CONFIG -> {
                R.string.onboarding_cta_connect
            }
        }
    Surface(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 14.dp)) {
            Button(
                onClick = onPrimaryClick,
                shapes = ButtonDefaults.shapes(),
                enabled = !state.isConnecting,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (state.isConnecting) {
                    LoadingIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(labelRes))
                }
            }
        }
    }
}

private fun stepSlideTransition(
    forward: Boolean,
    speed: AnimationSpeed,
): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val spatialOffsetSpec = speed.spatialSpecFor<IntOffset>()
        val effectsFloatSpec = speed.effectsSpecFor<Float>()
        val sign = if (forward) 1 else -1
        (fadeIn(effectsFloatSpec) + slideInHorizontally(spatialOffsetSpec) { sign * it / STEP_SLIDE_DIVISOR }) togetherWith
            (fadeOut(effectsFloatSpec) + slideOutHorizontally(spatialOffsetSpec) { -sign * it / STEP_SLIDE_DIVISOR })
    }

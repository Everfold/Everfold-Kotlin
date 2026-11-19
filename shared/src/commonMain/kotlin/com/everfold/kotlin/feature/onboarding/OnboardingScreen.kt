package com.everfold.kotlin.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionPreviewResult
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.AppearancePreferences
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.optionalLayerBackdrop
import com.everfold.kotlin.ui.theme.rememberOptionalLayerBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.continue_setup_shortcut
import everfold.shared.generated.resources.enter_home
import everfold.shared.generated.resources.next_step
import everfold.shared.generated.resources.onboarding_welcome
import everfold.shared.generated.resources.previous_step
import everfold.shared.generated.resources.previous_step_shortcut
import everfold.shared.generated.resources.retry_connection
import everfold.shared.generated.resources.setup_cluster
import everfold.shared.generated.resources.setup_mode
import everfold.shared.generated.resources.setup_preferences
import everfold.shared.generated.resources.test_connection
import everfold.shared.generated.resources.testing_connection
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class SetupStep { Welcome, Preferences, Mode, Cluster }

private val SetupStep.title
    get() =
        when (this) {
            SetupStep.Welcome -> Res.string.onboarding_welcome
            SetupStep.Preferences -> Res.string.setup_preferences
            SetupStep.Mode -> Res.string.setup_mode
            SetupStep.Cluster -> Res.string.setup_cluster
        }

internal enum class ConnectionTest { Idle, Testing, Success, Failure }

private const val CONNECTION_TEST_DELAY_MS = 900L
private const val OFFLINE_STEP_COUNT = 3f
private const val CLUSTER_STEP_COUNT = 4f
private const val PROGRESS_ANIM_MS = 240
private const val SLIDE_ANIM_MS = 240
private const val SLIDE_OUT_MS = 200
private const val FADE_IN_MS = 160
private const val FADE_IN_DELAY_MS = 40
private const val FADE_OUT_MS = 100

private data class OnboardingUiState(
    val step: SetupStep = SetupStep.Welcome,
    val cluster: Boolean = false,
    val form: ClusterSetup = ClusterSetup(),
    val test: ConnectionTest = ConnectionTest.Idle,
    val previewResult: ConnectionPreviewResult = ConnectionPreviewResult.Success,
    val showError: Boolean = false,
) {
    val isFinishStep: Boolean
        get() = step == SetupStep.Cluster || (step == SetupStep.Mode && !cluster)

    fun navigateTo(target: SetupStep): OnboardingUiState =
        copy(
            step = target,
            test = if (test == ConnectionTest.Testing) ConnectionTest.Idle else test,
        )

    fun updateForm(next: ClusterSetup): OnboardingUiState =
        copy(
            form = next,
            showError = form.method == next.method && form.auth == next.auth && showError,
            test = ConnectionTest.Idle,
        )
}

/** 首次设置向导：居中单列 Miuix 卡片与单层 Backdrop。 */
@Composable
internal fun OnboardingScreen(
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onComplete: (ClusterSetup?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var ui by remember { mutableStateOf(OnboardingUiState()) }
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(ui.test, ui.form, ui.previewResult, ui.step) {
        if (ui.test == ConnectionTest.Testing) {
            delay(CONNECTION_TEST_DELAY_MS)
            val result = if (ui.previewResult == ConnectionPreviewResult.Success) ConnectionTest.Success else ConnectionTest.Failure
            ui = ui.copy(test = result)
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        if (!hasFocus) focusRequester.requestFocus()
    }
    val back = { ui = ui.navigateTo(SetupStep.entries[(ui.step.ordinal - 1).coerceAtLeast(0)]) }
    val advance = { ui = advanceOnboarding(ui, onComplete) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = ui.step != SetupStep.Welcome,
        onBackCompleted = back,
    )
    val backdrop = rememberOptionalLayerBackdrop()
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event -> handleOnboardingKey(event, ui.step, ui.test, back, advance) }
                .onFocusChanged { hasFocus = it.hasFocus }
                .focusRequester(focusRequester)
                .focusable(),
        contentWindowInsets = WindowInsets(0),
    ) {
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            Box(
                Modifier
                    .fillMaxSize()
                    .optionalLayerBackdrop(backdrop)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                OnboardingContent(
                    ui = ui,
                    appearance = appearance,
                    language = language,
                    onLanguageChange = onLanguageChange,
                    onUiChange = { ui = it },
                    onBack = back,
                    onAdvance = advance,
                )
            }
        }
    }
}

private fun advanceOnboarding(
    ui: OnboardingUiState,
    onComplete: (ClusterSetup?) -> Unit,
): OnboardingUiState =
    when {
        ui.step == SetupStep.Cluster && ui.test != ConnectionTest.Success -> {
            val invalid = !ui.form.isValid
            val nextTest = if (!invalid && ui.test != ConnectionTest.Testing) ConnectionTest.Testing else ui.test
            ui.copy(showError = invalid, test = nextTest)
        }

        ui.isFinishStep -> {
            onComplete(if (ui.cluster) ui.form else null)
            ui
        }

        else -> {
            ui.navigateTo(SetupStep.entries[ui.step.ordinal + 1])
        }
    }

private fun handleOnboardingKey(
    event: KeyEvent,
    step: SetupStep,
    test: ConnectionTest,
    back: () -> Unit,
    advance: () -> Unit,
): Boolean =
    when {
        event.type != KeyEventType.KeyDown -> {
            false
        }

        (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.Enter -> {
            if (test != ConnectionTest.Testing) advance()
            true
        }

        event.isAltPressed && event.key == Key.DirectionLeft && step != SetupStep.Welcome -> {
            back()
            true
        }

        else -> {
            false
        }
    }

@Composable
private fun OnboardingContent(
    ui: OnboardingUiState,
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onUiChange: (OnboardingUiState) -> Unit,
    onBack: () -> Unit,
    onAdvance: () -> Unit,
) {
    val stepCount = if (ui.cluster) CLUSTER_STEP_COUNT else OFFLINE_STEP_COUNT
    val progress by animateFloatAsState(
        targetValue = (ui.step.ordinal + 1) / stepCount,
        animationSpec = tween(PROGRESS_ANIM_MS),
        label = "setupProgress",
    )
    Column(Modifier.widthIn(max = 600.dp).fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = localizedString(ui.step.title), style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.SemiBold)
            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
        }
        OnboardingAnimatedStep(
            ui = ui,
            appearance = appearance,
            language = language,
            onLanguageChange = onLanguageChange,
            onUiChange = onUiChange,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider()
        OnboardingFooter(
            step = ui.step,
            test = ui.test,
            finish = ui.isFinishStep,
            onBack = onBack,
            onAdvance = onAdvance,
        )
    }
}

@Composable
private fun OnboardingAnimatedStep(
    ui: OnboardingUiState,
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onUiChange: (OnboardingUiState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slideDistance = with(LocalDensity.current) { 32.dp.roundToPx() }
    AnimatedContent(
        targetState = ui.step,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (
                slideInHorizontally(tween(SLIDE_ANIM_MS)) { direction * slideDistance } +
                    fadeIn(tween(FADE_IN_MS, delayMillis = FADE_IN_DELAY_MS))
            ).togetherWith(slideOutHorizontally(tween(SLIDE_OUT_MS)) { -direction * slideDistance } + fadeOut(tween(FADE_OUT_MS)))
                .using(SizeTransform { _, _ -> tween(SLIDE_ANIM_MS) })
        },
        contentAlignment = Alignment.TopStart,
        label = "setupStep",
    ) { displayedStep ->
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (displayedStep) {
                SetupStep.Welcome -> {
                    OnboardingWelcomeStep()
                }

                SetupStep.Preferences -> {
                    OnboardingPreferencesStep(appearance, language, onLanguageChange)
                }

                SetupStep.Mode -> {
                    OnboardingModeStep(ui.cluster) { onUiChange(ui.copy(cluster = it)) }
                }

                SetupStep.Cluster -> {
                    OnboardingClusterStep(
                        form = ui.form,
                        showError = ui.showError,
                        previewResult = ui.previewResult,
                        test = ui.test,
                        onFormChange = { onUiChange(ui.updateForm(it)) },
                        onPreviewResultChange = { onUiChange(ui.copy(previewResult = it, test = ConnectionTest.Idle)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingFooter(
    step: SetupStep,
    test: ConnectionTest,
    finish: Boolean,
    onBack: () -> Unit,
    onAdvance: () -> Unit,
) {
    val actionLabel =
        when {
            step == SetupStep.Cluster && test == ConnectionTest.Testing -> Res.string.testing_connection
            step == SetupStep.Cluster && test == ConnectionTest.Failure -> Res.string.retry_connection
            step == SetupStep.Cluster && test != ConnectionTest.Success -> Res.string.test_connection
            finish -> Res.string.enter_home
            else -> Res.string.next_step
        }
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTooltip(localizedString(if (desktopUi) Res.string.previous_step_shortcut else Res.string.previous_step)) {
            TextButton(
                text = localizedString(Res.string.previous_step),
                onClick = onBack,
                enabled = step != SetupStep.Welcome,
                colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                insideMargin = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        AppTooltip(localizedString(if (desktopUi) Res.string.continue_setup_shortcut else actionLabel)) {
            Button(
                onClick = onAdvance,
                enabled = test != ConnectionTest.Testing,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(text = localizedString(actionLabel), style = MiuixTheme.textStyles.button)
            }
        }
    }
}

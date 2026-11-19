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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.feature.connections.ClusterConnectionForm
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionPreviewOptions
import com.everfold.kotlin.feature.connections.ConnectionPreviewResult
import com.everfold.kotlin.feature.connections.description
import com.everfold.kotlin.feature.settings.AppearanceSettings
import com.everfold.kotlin.feature.settings.LanguageSettings
import com.everfold.kotlin.feature.settings.ThemeColorSettings
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.AppearancePreferences
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.optionalLayerBackdrop
import com.everfold.kotlin.ui.theme.rememberOptionalLayerBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_success
import everfold.shared.generated.resources.continue_setup_shortcut
import everfold.shared.generated.resources.enter_home
import everfold.shared.generated.resources.next_step
import everfold.shared.generated.resources.onboarding_welcome
import everfold.shared.generated.resources.previous_step
import everfold.shared.generated.resources.previous_step_shortcut
import everfold.shared.generated.resources.retry_connection
import everfold.shared.generated.resources.setup_cluster
import everfold.shared.generated.resources.setup_mode
import everfold.shared.generated.resources.setup_nav_preferences
import everfold.shared.generated.resources.setup_nav_welcome
import everfold.shared.generated.resources.setup_preferences
import everfold.shared.generated.resources.test_connection
import everfold.shared.generated.resources.testing_connection
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.VerticalDivider
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 首次设置步骤欢迎偏好模式以及可选的集群配置 */
private enum class SetupStep { Welcome, Preferences, Mode, Cluster }

private val SetupStep.title
    get() =
        when (this) {
            SetupStep.Welcome -> Res.string.onboarding_welcome
            SetupStep.Preferences -> Res.string.setup_preferences
            SetupStep.Mode -> Res.string.setup_mode
            SetupStep.Cluster -> Res.string.setup_cluster
        }

private enum class ConnectionTest { Idle, Testing, Success, Failure }

private const val CONNECTION_TEST_DELAY_MS = 900L

private data class OnboardingUiState(
    val step: SetupStep = SetupStep.Welcome,
    val furthestStep: Int = 0,
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
            furthestStep = maxOf(furthestStep, target.ordinal),
            test = if (test == ConnectionTest.Testing) ConnectionTest.Idle else test,
        )

    fun updateForm(next: ClusterSetup): OnboardingUiState =
        copy(
            form = next,
            showError = form.method == next.method && form.auth == next.auth && showError,
            test = ConnectionTest.Idle,
        )
}

/** 首次设置向导协调步骤偏好与模拟连接测试完成后回传配置 */
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
            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .optionalLayerBackdrop(backdrop)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding(),
            ) {
                val wide = maxWidth >= 1000.dp
                LaunchedEffect(wide) {
                    withFrameNanos { }
                    if (!hasFocus) focusRequester.requestFocus()
                }
                OnboardingBody(
                    ui = ui,
                    wide = wide,
                    maxWidth = maxWidth,
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
private fun OnboardingBody(
    ui: OnboardingUiState,
    wide: Boolean,
    maxWidth: Dp,
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onUiChange: (OnboardingUiState) -> Unit,
    onBack: () -> Unit,
    onAdvance: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = (ui.step.ordinal + 1) / (if (ui.cluster) 4f else 3f),
        animationSpec = tween(240),
        label = "setupProgress",
    )
    val contentWidth = maxWidth - (if (wide) 280.dp + 64.dp else 48.dp) - 12.dp
    Row(Modifier.fillMaxSize()) {
        if (wide) {
            SetupSidebar(ui.step, ui.furthestStep, ui.cluster) { onUiChange(ui.navigateTo(it)) }
        }
        Column(Modifier.weight(1f).fillMaxHeight().padding(horizontal = if (wide) 32.dp else 24.dp, vertical = 20.dp)) {
            if (!wide) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            }
            OnboardingAnimatedStep(
                ui = ui,
                contentWidth = contentWidth,
                appearance = appearance,
                language = language,
                onLanguageChange = onLanguageChange,
                onClusterChange = { onUiChange(ui.copy(cluster = it)) },
                onFormChange = { onUiChange(ui.updateForm(it)) },
                onPreviewResultChange = { onUiChange(ui.copy(previewResult = it, test = ConnectionTest.Idle)) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            HorizontalDivider()
            OnboardingFooter(
                wide = wide,
                step = ui.step,
                test = ui.test,
                finish = ui.isFinishStep,
                onBack = onBack,
                onAdvance = onAdvance,
            )
        }
    }
}

@Composable
private fun OnboardingAnimatedStep(
    ui: OnboardingUiState,
    contentWidth: Dp,
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onClusterChange: (Boolean) -> Unit,
    onFormChange: (ClusterSetup) -> Unit,
    onPreviewResultChange: (ConnectionPreviewResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slideDistance = with(LocalDensity.current) { 32.dp.roundToPx() }
    val preferenceColumns =
        when {
            contentWidth >= 1200.dp -> 3
            contentWidth >= 720.dp -> 2
            else -> 1
        }
    AnimatedContent(
        targetState = ui.step,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally(tween(240)) { direction * slideDistance } + fadeIn(tween(160, delayMillis = 40)))
                .togetherWith(slideOutHorizontally(tween(200)) { -direction * slideDistance } + fadeOut(tween(100)))
                .using(SizeTransform { _, _ -> tween(240) })
        },
        contentAlignment = Alignment.TopStart,
        label = "setupStep",
    ) { displayedStep ->
        OnboardingScrollArea {
            Text(text = localizedString(displayedStep.title), style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.SemiBold)
            when (displayedStep) {
                SetupStep.Welcome -> {
                    WelcomeFeatures(contentWidth >= 840.dp)
                }

                SetupStep.Preferences -> {
                    val colWidth = (contentWidth - 24.dp * (preferenceColumns - 1)) / preferenceColumns
                    OnboardingPreferences(
                        appearance = appearance,
                        language = language,
                        onLanguageChange = onLanguageChange,
                        columns = preferenceColumns,
                        appearanceHorizontal = colWidth >= (380 * LocalDensity.current.fontScale).dp,
                    )
                }

                SetupStep.Mode -> {
                    ModeSelection(ui.cluster, onClusterChange, contentWidth >= 600.dp)
                }

                SetupStep.Cluster -> {
                    OnboardingClusterStep(
                        form = ui.form,
                        wide = contentWidth >= 600.dp,
                        showError = ui.showError,
                        previewResult = ui.previewResult,
                        test = ui.test,
                        onFormChange = onFormChange,
                        onPreviewResultChange = onPreviewResultChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingClusterStep(
    form: ClusterSetup,
    wide: Boolean,
    showError: Boolean,
    previewResult: ConnectionPreviewResult,
    test: ConnectionTest,
    onFormChange: (ClusterSetup) -> Unit,
    onPreviewResultChange: (ConnectionPreviewResult) -> Unit,
) {
    ClusterConnectionForm(form = form, wide = wide, onChange = onFormChange, showErrors = showError)
    ConnectionPreviewOptions(previewResult, onSelect = onPreviewResultChange)
    when (test) {
        ConnectionTest.Testing -> {
            LinearProgressIndicator(Modifier.fillMaxWidth(), progress = null)
        }

        ConnectionTest.Success -> {
            Text(
                text = localizedString(Res.string.connection_success),
                color = MiuixTheme.colorScheme.primary,
                style = MiuixTheme.textStyles.body2,
            )
        }

        ConnectionTest.Failure -> {
            Text(
                text = localizedString(previewResult.description),
                color = MiuixTheme.colorScheme.error,
                style = MiuixTheme.textStyles.body2,
            )
        }

        ConnectionTest.Idle -> {}
    }
}

@Composable
private fun OnboardingFooter(
    wide: Boolean,
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
        horizontalArrangement = if (wide) Arrangement.spacedBy(12.dp, Alignment.End) else Arrangement.SpaceBetween,
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

@Composable
private fun SetupSidebar(
    step: SetupStep,
    furthestStep: Int,
    cluster: Boolean,
    onNavigate: (SetupStep) -> Unit,
) {
    Surface(Modifier.width(280.dp).fillMaxHeight(), color = MiuixTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SetupStep.entries.take(if (cluster) 4 else 3).forEach { destination ->
                    val label =
                        when (destination) {
                            SetupStep.Welcome -> Res.string.setup_nav_welcome
                            SetupStep.Preferences -> Res.string.setup_nav_preferences
                            SetupStep.Mode -> Res.string.setup_mode
                            SetupStep.Cluster -> Res.string.setup_cluster
                        }
                    val enabled = destination.ordinal <= furthestStep
                    val selected = destination == step
                    val bg = if (selected) MiuixTheme.colorScheme.secondaryContainer else Color.Transparent
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .squircleSurface(color = bg, cornerRadius = 14.dp)
                                .selectable(selected = selected, enabled = enabled, role = Role.Tab) { onNavigate(destination) }
                                .alpha(if (enabled) 1f else 0.38f)
                                .focusProperties { canFocus = enabled }
                                .semantics { if (!enabled) disabled() }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = (destination.ordinal + 1).toString(),
                            style = MiuixTheme.textStyles.footnote1,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = localizedString(label),
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
            VerticalDivider()
        }
    }
}

@Composable
private fun OnboardingPreferences(
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    columns: Int,
    appearanceHorizontal: Boolean,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        maxItemsInEachRow = columns,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            AppearanceSettings(appearance.themeMode, appearance::setTheme, appearanceHorizontal)
            if (columns == 2) {
                ThemeColorSettings(
                    appearance.themeColor,
                    appearance::useSystemThemeColor,
                    appearance::selectThemePreset,
                    appearance::applyCustomThemeColor,
                )
            }
        }
        if (columns != 2) {
            Column(Modifier.weight(1f)) {
                ThemeColorSettings(
                    appearance.themeColor,
                    appearance::useSystemThemeColor,
                    appearance::selectThemePreset,
                    appearance::applyCustomThemeColor,
                )
            }
        }
        Column(Modifier.weight(1f)) {
            LanguageSettings(language, onLanguageChange)
        }
    }
}

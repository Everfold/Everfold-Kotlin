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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.feature.connections.ClusterConnectionForm
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ClusterSetupSaver
import com.everfold.kotlin.feature.connections.ConnectionPreviewOptions
import com.everfold.kotlin.feature.connections.ConnectionPreviewResult
import com.everfold.kotlin.feature.connections.description
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.components.WindowBreakpoints
import com.everfold.kotlin.ui.components.backShortcutModifier
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.components.primaryShortcutModifier
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.AppearancePreferences
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
import everfold.shared.generated.resources.setup_nav_permissions
import everfold.shared.generated.resources.setup_nav_preferences
import everfold.shared.generated.resources.setup_nav_welcome
import everfold.shared.generated.resources.setup_permissions
import everfold.shared.generated.resources.setup_preferences
import everfold.shared.generated.resources.test_connection
import everfold.shared.generated.resources.testing_connection
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource

private const val CONNECTION_TEST_DELAY_MILLIS = 900L
private const val STEP_PROGRESS_DURATION_MILLIS = 240
private const val STEP_FADE_IN_DURATION_MILLIS = 160
private const val STEP_FADE_IN_DELAY_MILLIS = 40
private const val STEP_SLIDE_OUT_DURATION_MILLIS = 200
private const val STEP_FADE_OUT_DURATION_MILLIS = 100
private const val DISABLED_STEP_ALPHA = 0.38f
private const val PREFERENCE_WIDE_COLUMNS = 3
private const val PREFERENCE_COMPACT_COLUMNS = 1
private const val APPEARANCE_ROW_MIN_WIDTH = 380f
private val PreferenceWideThreshold = 1200.dp
private val PreferenceMediumThreshold = 720.dp
private val WelcomeWideThreshold = 840.dp
private val SidebarWidth = 280.dp
private val SidebarContentReserve = 356.dp
private val CompactContentReserve = 60.dp
private val SlideOffsetDistance = 32.dp

/** 首次设置步骤：欢迎、偏好、（平台权限）、模式与可选集群配置。 */
private enum class SetupStep(
    val navLabel: StringResource,
    val title: StringResource,
) {
    Welcome(Res.string.setup_nav_welcome, Res.string.onboarding_welcome),
    Preferences(Res.string.setup_nav_preferences, Res.string.setup_preferences),
    Permissions(Res.string.setup_nav_permissions, Res.string.setup_permissions),
    Mode(Res.string.setup_mode, Res.string.setup_mode),
    Cluster(Res.string.setup_cluster, Res.string.setup_cluster),
}

private enum class ConnectionTest { Idle, Testing, Success, Failure }

private class OnboardingWizardState(
    val step: SetupStep,
    val furthestStep: Int,
    val cluster: Boolean,
    val hasPermissions: Boolean,
    val form: ClusterSetup,
    val test: ConnectionTest,
    val previewResult: ConnectionPreviewResult,
    val showError: Boolean,
) {
    val visibleSteps: List<SetupStep>
        get() = SetupStep.entries.filter { (it != SetupStep.Permissions || hasPermissions) && (it != SetupStep.Cluster || cluster) }
}

private class OnboardingActions(
    val onNavigate: (SetupStep) -> Unit,
    val onBack: () -> Unit,
    val onAdvance: () -> Unit,
    val onClusterChange: (Boolean) -> Unit,
    val onFormChange: (ClusterSetup) -> Unit,
    val onPreviewSelect: (ConnectionPreviewResult) -> Unit,
    val onLanguageChange: (AppLanguage) -> Unit,
)

/** 首次设置向导协调步骤偏好与模拟连接测试完成后回传配置 */
@Composable
internal fun OnboardingScreen(
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onComplete: (ClusterSetup?) -> Unit,
    modifier: Modifier = Modifier,
    permissions: OnboardingPermissionsController = rememberOnboardingPermissionsController(),
) {
    val holder = rememberOnboardingWizardHolder(permissions.items.isNotEmpty(), onComplete, onLanguageChange)
    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = holder.wizard.step != SetupStep.Welcome,
        onBackCompleted = holder.actions.onBack,
    )
    Surface(
        modifier
            .fillMaxSize()
            .onboardingShortcuts(
                canGoBack = holder.wizard.step != SetupStep.Welcome,
                testing = holder.wizard.test == ConnectionTest.Testing,
                onBack = holder.actions.onBack,
                onAdvance = holder.actions.onAdvance,
            ).onFocusChanged { hasFocus = it.hasFocus }
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        OnboardingScaffold(holder.wizard, permissions, appearance, language, holder.actions, focusRequester, hasFocus)
    }
}

private class OnboardingWizardHolder(
    val wizard: OnboardingWizardState,
    val actions: OnboardingActions,
)

@Composable
private fun rememberOnboardingWizardHolder(
    hasPermissions: Boolean,
    onComplete: (ClusterSetup?) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
): OnboardingWizardHolder {
    var step by rememberSaveable { mutableStateOf(SetupStep.Welcome) }
    var furthestStep by rememberSaveable { mutableIntStateOf(0) }
    var cluster by rememberSaveable { mutableStateOf(false) }
    var form by rememberSaveable(stateSaver = ClusterSetupSaver) { mutableStateOf(ClusterSetup()) }
    var test by remember { mutableStateOf(ConnectionTest.Idle) }
    var previewResult by rememberSaveable { mutableStateOf(ConnectionPreviewResult.Success) }
    var showError by rememberSaveable { mutableStateOf(false) }
    OnboardingConnectionTestEffect(test, form, previewResult, step) { test = it }
    val activeSteps =
        remember(cluster, hasPermissions) {
            SetupStep.entries.filter { (it != SetupStep.Permissions || hasPermissions) && (it != SetupStep.Cluster || cluster) }
        }
    val navigate: (SetupStep) -> Unit = {
        if (test == ConnectionTest.Testing) test = ConnectionTest.Idle
        step = it
        furthestStep = maxOf(furthestStep, it.ordinal)
    }
    val stepIdx = activeSteps.indexOf(step).coerceAtLeast(0)
    val back = { navigate(activeSteps[(stepIdx - 1).coerceAtLeast(0)]) }
    val advance = {
        advanceOnboardingStep(
            step = step,
            cluster = cluster,
            form = form,
            test = test,
            onShowError = { showError = it },
            onStartTest = { test = ConnectionTest.Testing },
            onNavigateNext = { if (stepIdx + 1 < activeSteps.size) navigate(activeSteps[stepIdx + 1]) },
            onComplete = { onComplete(if (cluster) form else null) },
        )
    }
    val wizard = OnboardingWizardState(step, furthestStep, cluster, hasPermissions, form, test, previewResult, showError)
    val actions =
        OnboardingActions(
            onNavigate = navigate,
            onBack = back,
            onAdvance = advance,
            onClusterChange = { cluster = it },
            onFormChange = {
                if (form.method != it.method || form.auth != it.auth) showError = false
                form = it
                test = ConnectionTest.Idle
            },
            onPreviewSelect = {
                previewResult = it
                test = ConnectionTest.Idle
            },
            onLanguageChange = onLanguageChange,
        )
    return OnboardingWizardHolder(wizard, actions)
}

@Composable
private fun OnboardingConnectionTestEffect(
    test: ConnectionTest,
    form: ClusterSetup,
    previewResult: ConnectionPreviewResult,
    step: SetupStep,
    onTestComplete: (ConnectionTest) -> Unit,
) {
    val currentOnTestComplete by rememberUpdatedState(onTestComplete)
    LaunchedEffect(test, form, previewResult, step) {
        if (test == ConnectionTest.Testing) {
            delay(CONNECTION_TEST_DELAY_MILLIS)
            currentOnTestComplete(if (previewResult == ConnectionPreviewResult.Success) ConnectionTest.Success else ConnectionTest.Failure)
        }
    }
}

private fun advanceOnboardingStep(
    step: SetupStep,
    cluster: Boolean,
    form: ClusterSetup,
    test: ConnectionTest,
    onShowError: (Boolean) -> Unit,
    onStartTest: () -> Unit,
    onNavigateNext: () -> Unit,
    onComplete: () -> Unit,
) {
    val finish = step == SetupStep.Cluster || (step == SetupStep.Mode && !cluster)
    when {
        step == SetupStep.Cluster && test != ConnectionTest.Success -> {
            onShowError(!form.isValid)
            if (form.isValid && test != ConnectionTest.Testing) onStartTest()
        }

        finish -> {
            onComplete()
        }

        else -> {
            onNavigateNext()
        }
    }
}

@Composable
private fun OnboardingScaffold(
    wizard: OnboardingWizardState,
    permissions: OnboardingPermissionsController,
    appearance: AppearancePreferences,
    language: AppLanguage,
    actions: OnboardingActions,
    focusRequester: FocusRequester,
    hasFocus: Boolean,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WindowBreakpoints.OnboardingWide
        val visibleSteps = wizard.visibleSteps
        val currentIndex = visibleSteps.indexOf(wizard.step).coerceAtLeast(0)
        val progress by animateFloatAsState(
            targetValue = (currentIndex + 1) / visibleSteps.size.toFloat(),
            animationSpec = tween(STEP_PROGRESS_DURATION_MILLIS),
            label = "setupProgress",
        )
        val contentWidth = maxWidth - (if (wide) SidebarContentReserve else CompactContentReserve)
        LaunchedEffect(wide) {
            withFrameNanos { }
            if (!hasFocus) focusRequester.requestFocus()
        }
        val contentInsets =
            WindowInsets.safeDrawing.only(
                if (wide) WindowInsetsSides.Vertical + WindowInsetsSides.End else WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal,
            )
        Row(Modifier.fillMaxSize()) {
            if (wide) SetupSidebar(wizard.step, wizard.furthestStep, visibleSteps, actions.onNavigate)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(contentInsets)
                    .padding(horizontal = if (wide) 32.dp else 24.dp, vertical = 20.dp),
            ) {
                if (!wide) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                OnboardingStepContent(
                    wizard = wizard,
                    permissions = permissions,
                    appearance = appearance,
                    language = language,
                    contentWidth = contentWidth,
                    actions = actions,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                HorizontalDivider()
                OnboardingFooter(wizard, wide, actions.onBack, actions.onAdvance)
            }
        }
    }
}

@Composable
private fun OnboardingStepContent(
    wizard: OnboardingWizardState,
    permissions: OnboardingPermissionsController,
    appearance: AppearancePreferences,
    language: AppLanguage,
    contentWidth: Dp,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    val slideDistance = with(LocalDensity.current) { SlideOffsetDistance.roundToPx() }
    val preferenceColumns = preferenceColumnCount(contentWidth)
    AnimatedContent(
        targetState = wizard.step,
        modifier = modifier,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (
                slideInHorizontally(tween(STEP_PROGRESS_DURATION_MILLIS)) { direction * slideDistance } +
                    fadeIn(tween(STEP_FADE_IN_DURATION_MILLIS, delayMillis = STEP_FADE_IN_DELAY_MILLIS))
            ).togetherWith(
                slideOutHorizontally(tween(STEP_SLIDE_OUT_DURATION_MILLIS)) { -direction * slideDistance } +
                    fadeOut(tween(STEP_FADE_OUT_DURATION_MILLIS)),
            ).using(SizeTransform { _, _ -> tween(STEP_PROGRESS_DURATION_MILLIS) })
        },
        contentAlignment = Alignment.TopStart,
        label = "setupStep",
    ) { displayedStep ->
        OnboardingScrollArea {
            Text(localizedString(displayedStep.title), style = MaterialTheme.typography.headlineMedium)
            when (displayedStep) {
                SetupStep.Welcome -> {
                    WelcomeFeatures(contentWidth >= WelcomeWideThreshold)
                }

                SetupStep.Preferences -> {
                    OnboardingPreferences(
                        appearance = appearance,
                        language = language,
                        onLanguageChange = actions.onLanguageChange,
                        columns = preferenceColumns,
                        appearanceHorizontal =
                            (contentWidth - 24.dp * (preferenceColumns - 1)) / preferenceColumns >=
                                (APPEARANCE_ROW_MIN_WIDTH * LocalDensity.current.fontScale).dp,
                    )
                }

                SetupStep.Permissions -> {
                    OnboardingPermissionsStep(
                        items = permissions.items,
                        onRequest = permissions::request,
                        onManage = permissions::openSettings,
                    )
                }

                SetupStep.Mode -> {
                    ModeSelection(wizard.cluster, actions.onClusterChange, contentWidth >= WindowBreakpoints.Compact)
                }

                SetupStep.Cluster -> {
                    OnboardingClusterStep(wizard, contentWidth >= WindowBreakpoints.Compact, actions)
                }
            }
        }
    }
}

@Composable
private fun OnboardingClusterStep(
    wizard: OnboardingWizardState,
    wideForm: Boolean,
    actions: OnboardingActions,
) {
    ClusterConnectionForm(wizard.form, wide = wideForm, showErrors = wizard.showError, onChange = actions.onFormChange)
    ConnectionPreviewOptions(wizard.previewResult, onSelect = actions.onPreviewSelect)
    when (wizard.test) {
        ConnectionTest.Testing -> {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        ConnectionTest.Success -> {
            Text(localizedString(Res.string.connection_success), color = MaterialTheme.colorScheme.primary)
        }

        ConnectionTest.Failure -> {
            Text(localizedString(wizard.previewResult.description), color = MaterialTheme.colorScheme.error)
        }

        ConnectionTest.Idle -> {}
    }
}

@Composable
private fun OnboardingFooter(
    wizard: OnboardingWizardState,
    wide: Boolean,
    onBack: () -> Unit,
    onAdvance: () -> Unit,
) {
    val finish = wizard.step == SetupStep.Cluster || (wizard.step == SetupStep.Mode && !wizard.cluster)
    val actionLabel =
        when {
            wizard.step == SetupStep.Cluster && wizard.test == ConnectionTest.Testing -> Res.string.testing_connection
            wizard.step == SetupStep.Cluster && wizard.test == ConnectionTest.Failure -> Res.string.retry_connection
            wizard.step == SetupStep.Cluster && wizard.test != ConnectionTest.Success -> Res.string.test_connection
            finish -> Res.string.enter_home
            else -> Res.string.next_step
        }
    val backTooltip =
        if (desktopUi) {
            localizedString(Res.string.previous_step_shortcut, backShortcutModifier)
        } else {
            localizedString(Res.string.previous_step)
        }
    val advanceTooltip =
        if (desktopUi) {
            localizedString(Res.string.continue_setup_shortcut, primaryShortcutModifier)
        } else {
            localizedString(actionLabel)
        }
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = if (wide) Arrangement.spacedBy(12.dp, Alignment.End) else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTooltip(backTooltip) {
            TextButton(onClick = onBack, enabled = wizard.step != SetupStep.Welcome) {
                Text(localizedString(Res.string.previous_step))
            }
        }
        AppTooltip(advanceTooltip) {
            Button(onClick = onAdvance, enabled = wizard.test != ConnectionTest.Testing) {
                Text(localizedString(actionLabel))
            }
        }
    }
}

private fun preferenceColumnCount(contentWidth: Dp): Int =
    when {
        contentWidth >= PreferenceWideThreshold -> PREFERENCE_WIDE_COLUMNS
        contentWidth >= PreferenceMediumThreshold -> PREFERENCE_MEDIUM_COLUMNS
        else -> PREFERENCE_COMPACT_COLUMNS
    }

private fun Modifier.onboardingShortcuts(
    canGoBack: Boolean,
    testing: Boolean,
    onBack: () -> Unit,
    onAdvance: () -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
            (event.isCtrlPressed || event.isMetaPressed) && event.key == Key.Enter -> {
                if (!testing) onAdvance()
                true
            }

            event.isAltPressed && event.key == Key.DirectionLeft && canGoBack -> {
                onBack()
                true
            }

            else -> {
                false
            }
        }
    }

@Composable
private fun SetupSidebar(
    step: SetupStep,
    furthestStep: Int,
    visibleSteps: List<SetupStep>,
    onNavigate: (SetupStep) -> Unit,
) {
    PermanentDrawerSheet(
        Modifier.width(SidebarWidth).fillMaxHeight(),
        drawerShape = RectangleShape,
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start),
    ) {
        Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            visibleSteps.forEachIndexed { index, destination ->
                // 侧栏仅允许返回已到达的步骤避免绕过配置流程
                val enabled = destination.ordinal <= furthestStep
                NavigationDrawerItem(
                    label = { Text(localizedString(destination.navLabel)) },
                    selected = destination == step,
                    onClick = { if (enabled) onNavigate(destination) },
                    modifier =
                        Modifier
                            .alpha(if (enabled) 1f else DISABLED_STEP_ALPHA)
                            .focusProperties { canFocus = enabled }
                            .semantics { if (!enabled) disabled() },
                    icon = { Text((index + 1).toString()) },
                )
            }
        }
    }
}

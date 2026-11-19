package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.optionalLayerBackdrop
import com.everfold.kotlin.ui.theme.rememberOptionalLayerBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.nav_settings
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.glass.GlassTopAppBar

/** 宽屏设置页面：独立单层 Scaffold 与 OS4 玻璃大标题栏。 */
@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val backdrop = rememberOptionalLayerBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            GlassTopAppBar(
                title = localizedString(Res.string.nav_settings),
                expandable = true,
                progressiveBlur = true,
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val contentPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), navBottom),
            )
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            SettingsListContent(
                state = state,
                actions = actions,
                contentPadding = contentPadding,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .optionalLayerBackdrop(backdrop)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
            )
        }
    }
}

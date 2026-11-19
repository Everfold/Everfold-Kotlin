package com.everfold.kotlin.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.back_to_conversation
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 连接与设置页共用布局：渐进模糊大标题顶栏与居中限宽可滚动正文 */
@Composable
fun SettingsLayout(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    scrollState: ScrollState = rememberScrollState(),
    bottomPadding: Dp = 0.dp,
    navigation: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SettingsTopBar(title, backdrop, scrollBehavior, onBack) },
    ) { innerPadding ->
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            Box(
                Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 760.dp)
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()),
                ) {
                    navigation()
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                            .verticalScroll(scrollState)
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 16.dp,
                                bottom = 24.dp + bottomPadding + innerPadding.calculateBottomPadding(),
                            ),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    backdrop: Backdrop,
    scrollBehavior: ScrollBehavior,
    onBack: (() -> Unit)?,
) {
    GlassTopAppBar(
        title = title,
        expandable = true,
        progressiveBlur = true,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            if (onBack != null) {
                GlassIconButton(onClick = onBack) {
                    // 返回会话
                    AppIcon(
                        AppSymbol.Back,
                        modifier = Modifier.size(28.dp),
                        description = localizedString(Res.string.back_to_conversation),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
}

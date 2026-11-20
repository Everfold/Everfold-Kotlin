package com.everfold.kotlin.ui.components

import androidx.compose.ui.unit.dp

/** 响应式布局宽度断点常量，避免各页面重复硬编码阈值。 */
internal object WindowBreakpoints {
    val Compact = 600.dp
    val Expanded = 900.dp
    val OnboardingWide = 1000.dp
}

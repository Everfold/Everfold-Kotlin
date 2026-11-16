package com.everfold.kotlin.app.navigation

import com.everfold.kotlin.ui.components.AppSymbol
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.nav_connections
import everfold.shared.generated.resources.nav_sessions
import everfold.shared.generated.resources.nav_settings
import org.jetbrains.compose.resources.StringResource

/** 顶层导航定义共用标签资源与图标供导航栏和抽屉渲染 */
enum class AppDestination(
    val label: StringResource,
    val icon: AppSymbol,
) {
    // 工作台
    Sessions(Res.string.nav_sessions, AppSymbol.Chat),

    // 连接
    Connections(Res.string.nav_connections, AppSymbol.Connection),

    // 设置
    Settings(Res.string.nav_settings, AppSymbol.Settings),
}

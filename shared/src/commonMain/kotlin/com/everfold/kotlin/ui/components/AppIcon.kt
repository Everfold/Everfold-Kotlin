package com.everfold.kotlin.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.Check
import top.yukonga.miuix.kmp.icon.glass.Add
import top.yukonga.miuix.kmp.icon.glass.Ai
import top.yukonga.miuix.kmp.icon.glass.ChevronBackward
import top.yukonga.miuix.kmp.icon.glass.Close
import top.yukonga.miuix.kmp.icon.glass.CollapseList
import top.yukonga.miuix.kmp.icon.glass.Delete
import top.yukonga.miuix.kmp.icon.glass.Edit
import top.yukonga.miuix.kmp.icon.glass.ExpandMore
import top.yukonga.miuix.kmp.icon.glass.File
import top.yukonga.miuix.kmp.icon.glass.FolderOutline
import top.yukonga.miuix.kmp.icon.glass.FullScreen
import top.yukonga.miuix.kmp.icon.glass.Hide
import top.yukonga.miuix.kmp.icon.glass.Menu
import top.yukonga.miuix.kmp.icon.glass.Messages
import top.yukonga.miuix.kmp.icon.glass.More
import top.yukonga.miuix.kmp.icon.glass.MoreConnections
import top.yukonga.miuix.kmp.icon.glass.OpenWithOtherApp
import top.yukonga.miuix.kmp.icon.glass.Pause
import top.yukonga.miuix.kmp.icon.glass.Pin
import top.yukonga.miuix.kmp.icon.glass.Refresh
import top.yukonga.miuix.kmp.icon.glass.Search
import top.yukonga.miuix.kmp.icon.glass.Send
import top.yukonga.miuix.kmp.icon.glass.SettingsOutline
import top.yukonga.miuix.kmp.icon.glass.Show
import top.yukonga.miuix.kmp.icon.glass.Sidebar
import top.yukonga.miuix.kmp.icon.glass.Unpin
import top.yukonga.miuix.kmp.theme.LocalContentColor

enum class AppSymbol(
    val imageVector: ImageVector,
) {
    Chat(MiuixIcons.Glass.Messages),
    Connection(MiuixIcons.Glass.MoreConnections),
    Settings(MiuixIcons.Glass.SettingsOutline),
    Folder(MiuixIcons.Glass.FolderOutline),
    Attachment(MiuixIcons.Glass.File),
    ChevronDown(MiuixIcons.Glass.ExpandMore),
    Check(MiuixIcons.Basic.Check),
    Close(MiuixIcons.Glass.Close),
    Retry(MiuixIcons.Glass.Refresh),
    Plus(MiuixIcons.Glass.Add),
    Back(MiuixIcons.Glass.ChevronBackward),
    Send(MiuixIcons.Glass.Send),
    Stop(MiuixIcons.Glass.Pause),
    Search(MiuixIcons.Glass.Search),
    Menu(MiuixIcons.Glass.Menu),
    Sidebar(MiuixIcons.Glass.Sidebar),
    OpenInNew(MiuixIcons.Glass.OpenWithOtherApp),
    Expand(MiuixIcons.Glass.FullScreen),
    Collapse(MiuixIcons.Glass.CollapseList),
    More(MiuixIcons.Glass.More),
    Spark(MiuixIcons.Glass.Ai),
    Edit(MiuixIcons.Glass.Edit),
    Delete(MiuixIcons.Glass.Delete),
    Pin(MiuixIcons.Glass.Pin),
    Unpin(MiuixIcons.Glass.Unpin),
    Show(MiuixIcons.Glass.Show),
    Hide(MiuixIcons.Glass.Hide),
}

@Composable
fun AppIcon(
    symbol: AppSymbol,
    modifier: Modifier = Modifier,
    description: String? = null,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = symbol.imageVector,
        contentDescription = description,
        modifier = modifier,
        tint = tint,
    )
}

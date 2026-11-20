package com.everfold.kotlin.mock.longtask

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.everfold.kotlin.mock.MockWorkspace
import java.util.Locale

internal actual val platformLongTaskBridge: PlatformLongTaskBridge = NoopLongTaskBridge

internal actual fun isEnglishNotificationLocale(): Boolean = Locale.getDefault().language.equals("en", ignoreCase = true)

@Composable
internal actual fun rememberAppWorkspace(): MockWorkspace =
    viewModel { MockWorkspace(enableLongTasks = true, taskBridge = platformLongTaskBridge) }

@Composable
internal actual fun PlatformLongTaskPermissionEffect(hasActiveLocalTask: Boolean) = Unit

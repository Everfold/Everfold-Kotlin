package com.everfold.kotlin.feature.settings

import androidx.compose.runtime.Composable

/** 读取平台提供的版本号无版本信息时返回 null由界面省略版本 */
@Composable
internal expect fun appVersion(): String?

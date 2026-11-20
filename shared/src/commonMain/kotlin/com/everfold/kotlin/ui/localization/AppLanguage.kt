@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.everfold.kotlin.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** 应用语言选项跟随系统时英文使用 en其他系统语言回退到中文 */
enum class AppLanguage { System, Chinese, English }

internal fun AppLanguage.resolve(systemTag: String): AppLanguage {
    if (this != AppLanguage.System) return this
    val systemLanguage = systemTag.substringBefore('-').substringBefore('_')
    return if (systemLanguage.equals("en", ignoreCase = true)) AppLanguage.English else AppLanguage.Chinese
}

private val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.Chinese }

/** 将解析后的语言应用于资源环境并向下提供当前语言 */
@Composable
internal fun AppLanguageEnvironment(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val overrideTag =
        when (language) {
            AppLanguage.System -> null
            AppLanguage.Chinese -> "zh"
            AppLanguage.English -> "en"
        }
    val resolved = language.resolve(applyAndResolveSystemLanguageTag(overrideTag))
    CompositionLocalProvider(LocalAppLanguage provides resolved, content = content)
}

// 仅重建资源查询切换语言时保留外围界面草稿和导航状态
@Composable
fun localizedString(
    resource: StringResource,
    vararg args: Any,
): String =
    key(LocalAppLanguage.current) {
        stringResource(resource, *args)
    }

@Composable
internal expect fun applyAndResolveSystemLanguageTag(overrideTag: String?): String

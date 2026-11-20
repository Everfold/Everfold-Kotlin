package com.everfold.kotlin.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
internal actual fun applyAndResolveSystemLanguageTag(overrideTag: String?): String {
    val locales = LocalConfiguration.current.locales
    val systemLocale = if (locales.isEmpty) Locale.getDefault() else locales[0]
    val target = overrideTag?.let(Locale::forLanguageTag) ?: systemLocale
    if (Locale.getDefault() != target) Locale.setDefault(target)
    return systemLocale.toLanguageTag()
}

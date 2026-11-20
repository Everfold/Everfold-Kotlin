package com.everfold.kotlin.ui.localization

import androidx.compose.runtime.Composable
import java.util.Locale

// Capture the OS/JVM locale before an in-app override is applied, while allowing dynamic updates.
private var systemLocale: Locale = Locale.getDefault()
private var appliedOverride: Locale? = null

@Composable
internal actual fun applyAndResolveSystemLanguageTag(overrideTag: String?): String = applyJvmResourceLocale(overrideTag)

internal fun applyJvmResourceLocale(overrideTag: String?): String {
    val current = Locale.getDefault()
    if (appliedOverride == null || current != appliedOverride) {
        systemLocale = current
        appliedOverride = null
    }
    val target =
        if (overrideTag == null) {
            appliedOverride = null
            systemLocale
        } else {
            Locale.forLanguageTag(overrideTag).also { appliedOverride = it }
        }
    if (Locale.getDefault() != target) Locale.setDefault(target)
    return systemLocale.toLanguageTag()
}

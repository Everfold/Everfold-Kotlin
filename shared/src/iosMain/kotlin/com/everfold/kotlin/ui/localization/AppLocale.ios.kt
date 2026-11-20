package com.everfold.kotlin.ui.localization

import androidx.compose.runtime.Composable
import platform.Foundation.NSArgumentDomain
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

private const val APPLE_LANGUAGES_KEY = "AppleLanguages"
private var appliedOverride: String? = null

@Composable
internal actual fun applyAndResolveSystemLanguageTag(overrideTag: String?): String {
    val defaults = NSUserDefaults.standardUserDefaults
    if (overrideTag == null) {
        if (appliedOverride != null) {
            val updated = defaults.volatileDomainForName(NSArgumentDomain).filterKeys { it != APPLE_LANGUAGES_KEY }
            defaults.setVolatileDomain(updated, forName = NSArgumentDomain)
            appliedOverride = null
        }
    } else if (appliedOverride != overrideTag) {
        val updated = defaults.volatileDomainForName(NSArgumentDomain) + mapOf<Any?, Any?>(APPLE_LANGUAGES_KEY to listOf(overrideTag))
        defaults.setVolatileDomain(updated, forName = NSArgumentDomain)
        appliedOverride = overrideTag
    }
    return NSLocale.preferredLanguages.firstOrNull() as? String ?: "zh"
}

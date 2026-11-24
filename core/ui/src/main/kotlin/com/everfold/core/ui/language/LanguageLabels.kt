package com.everfold.core.ui.language

import android.content.Context
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.everfold.core.model.AppLanguage
import com.everfold.core.ui.R
import java.util.Locale

private val TRADITIONAL_CHINESE_REGIONS = setOf("TW", "HK", "MO")

@StringRes
fun AppLanguage.nativeLabelRes(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.language_native_zh_hans
        AppLanguage.TRADITIONAL_CHINESE -> R.string.language_native_zh_hant
        AppLanguage.ENGLISH -> R.string.language_native_en
    }

@StringRes
fun AppLanguage.localizedLabelRes(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.language_localized_zh_hans
        AppLanguage.TRADITIONAL_CHINESE -> R.string.language_localized_zh_hant
        AppLanguage.ENGLISH -> R.string.language_localized_en
    }

@Composable
fun AppLanguage.summaryLabel(): String =
    if (this == AppLanguage.SYSTEM) {
        stringResource(R.string.language_summary_system, resolveSystemLanguageLabel(LocalContext.current))
    } else {
        stringResource(nativeLabelRes())
    }

fun resolveSystemLanguageLabel(context: Context): String {
    val systemLocale =
        Resources
            .getSystem()
            .configuration.locales
            .get(0) ?: Locale.getDefault()
    val isTraditionalChinese =
        systemLocale.language == "zh" &&
            (systemLocale.script == "Hant" || systemLocale.country in TRADITIONAL_CHINESE_REGIONS)
    return when {
        isTraditionalChinese -> context.getString(R.string.language_native_zh_hant)
        systemLocale.language == "zh" -> context.getString(R.string.language_native_zh_hans)
        systemLocale.language == "en" -> context.getString(R.string.language_native_en)
        else -> systemLocale.getDisplayName(systemLocale)
    }
}

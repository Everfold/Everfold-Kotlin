package com.everfold.core.designsystem.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.everfold.core.model.AppLanguage
import java.util.Locale

@Composable
internal fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    if (language == AppLanguage.SYSTEM) {
        content()
        return
    }

    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val (localizedContext, localizedConfig) =
        remember(language, baseContext, baseConfig) {
            val locale = Locale.forLanguageTag(language.languageTag)
            val config =
                Configuration(baseConfig).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
            baseContext.createConfigurationContext(config) to config
        }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfig,
        LocalContext provides localizedContext,
        LocalResources provides localizedContext.resources,
        content = content,
    )
}

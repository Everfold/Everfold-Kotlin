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
    val baseContext = LocalContext.current
    val baseConfig = LocalConfiguration.current
    val baseResources = LocalResources.current
    val (targetContext, targetConfig, targetResources) =
        remember(language, baseContext, baseConfig, baseResources) {
            if (language == AppLanguage.SYSTEM) {
                Triple(baseContext, baseConfig, baseResources)
            } else {
                val locale = Locale.forLanguageTag(language.languageTag)
                val config =
                    Configuration(baseConfig).apply {
                        setLocale(locale)
                        setLayoutDirection(locale)
                    }
                val localizedContext = baseContext.createConfigurationContext(config)
                Triple(localizedContext, config, localizedContext.resources)
            }
        }

    CompositionLocalProvider(
        LocalConfiguration provides targetConfig,
        LocalContext provides targetContext,
        LocalResources provides targetResources,
        content = content,
    )
}

package com.everfold.feature.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.model.AppLanguage
import com.everfold.feature.settings.R
import com.everfold.feature.settings.SettingsViewModel
import com.everfold.feature.settings.system.applyAppLanguage
import org.koin.androidx.compose.koinViewModel

@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsSubPageScaffold(
        title = stringResource(R.string.settings_entry_language_title),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        AppLanguage.entries.forEachIndexed { index, option ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            LanguageOptionRow(
                                option = option,
                                selected = prefs.language == option,
                                onClick = {
                                    viewModel.setLanguage(option)
                                    applyAppLanguage(context, option)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    option: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(option.labelRes()),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
    }
}

@StringRes
internal fun AppLanguage.labelRes(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.language_zh_hans
        AppLanguage.TRADITIONAL_CHINESE -> R.string.language_zh_hant
        AppLanguage.ENGLISH -> R.string.language_en
    }

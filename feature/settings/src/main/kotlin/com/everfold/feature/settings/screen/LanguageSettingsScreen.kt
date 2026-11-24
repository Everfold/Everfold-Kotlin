package com.everfold.feature.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.model.AppLanguage
import com.everfold.feature.settings.R
import com.everfold.feature.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val options = AppLanguage.entries

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
                EverfoldGroupedList {
                    options.forEachIndexed { index, option ->
                        val selected = prefs.language == option
                        EverfoldGroupedItem(
                            index = index,
                            count = options.size,
                            containerColor =
                                if (selected) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                            onClick = { viewModel.setLanguage(option) },
                        ) {
                            LanguageOptionRow(option = option, selected = selected)
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
) {
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = stringResource(option.labelRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
        }
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

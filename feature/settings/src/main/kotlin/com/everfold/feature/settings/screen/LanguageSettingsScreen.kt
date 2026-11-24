package com.everfold.feature.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.ui.language.LanguagePickerList
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
                LanguagePickerList(
                    selected = prefs.language,
                    onSelect = viewModel::setLanguage,
                )
            }
        }
    }
}

package com.everfold.core.ui.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.model.AppLanguage
import com.everfold.core.ui.R

private const val DEFAULT_LANGUAGE_SEARCH_THRESHOLD = 6

@Composable
fun LanguageEntryCard(
    selected: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EverfoldGroupedList(modifier = modifier) {
        EverfoldGroupedItem(index = 0, count = 1, onClick = onClick) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.language_label),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = selected.summaryLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun LanguagePickerList(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    searchThreshold: Int = DEFAULT_LANGUAGE_SEARCH_THRESHOLD,
) {
    val concreteLanguages = AppLanguage.entries.filterNot { it == AppLanguage.SYSTEM }
    val showSearch = concreteLanguages.size > searchThreshold
    var query by rememberSaveable { mutableStateOf("") }
    val resolvedItems =
        concreteLanguages.map { lang ->
            Triple(
                lang,
                stringResource(lang.nativeLabelRes()),
                stringResource(lang.localizedLabelRes()),
            )
        }
    val filtered =
        if (!showSearch || query.isBlank()) {
            resolvedItems
        } else {
            val trimmed = query.trim()
            resolvedItems.filter { (lang, nativeName, localizedName) ->
                nativeName.contains(trimmed, ignoreCase = true) ||
                    localizedName.contains(trimmed, ignoreCase = true) ||
                    lang.languageTag.contains(trimmed, ignoreCase = true)
            }
        }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PinnedSystemLanguageGroup(
            selected = selected == AppLanguage.SYSTEM,
            onSelect = { onSelect(AppLanguage.SYSTEM) },
        )
        if (showSearch) {
            LanguageSearchField(query = query, onQueryChange = { query = it })
        }
        if (filtered.isEmpty()) {
            Text(
                text = stringResource(R.string.language_empty_search),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            )
        } else {
            ConcreteLanguageGroup(
                items = filtered,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun PinnedSystemLanguageGroup(
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val systemLanguageName = resolveSystemLanguageLabel(LocalContext.current)
    EverfoldGroupedList {
        EverfoldGroupedItem(
            index = 0,
            count = 1,
            containerColor =
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            onClick = onSelect,
        ) {
            LanguageOptionRow(
                title = stringResource(R.string.language_system),
                subtitle = stringResource(R.string.language_system_current, systemLanguageName),
                selected = selected,
            )
        }
    }
}

@Composable
private fun ConcreteLanguageGroup(
    items: List<Triple<AppLanguage, String, String>>,
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    EverfoldGroupedList {
        items.forEachIndexed { index, (language, nativeName, localizedName) ->
            val isSelected = selected == language
            EverfoldGroupedItem(
                index = index,
                count = items.size,
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                onClick = { onSelect(language) },
            ) {
                LanguageOptionRow(
                    title = nativeName,
                    subtitle = localizedName.takeIf { it != nativeName },
                    selected = isSelected,
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
) {
    val contentColor =
        if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val subtitleColor =
        if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                )
            }
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LanguageSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.language_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LanguagePickerSheet(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.language_label),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
            LanguagePickerList(
                selected = selected,
                onSelect = { language ->
                    onSelect(language)
                    onDismiss()
                },
            )
        }
    }
}

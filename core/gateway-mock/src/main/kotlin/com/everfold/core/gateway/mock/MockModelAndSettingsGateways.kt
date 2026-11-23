package com.everfold.core.gateway.mock

import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.ModelGateway
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.ModelCapability
import com.everfold.core.model.ModelDescriptor
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

class MockModelGateway(
    private val hostNameProvider: () -> String,
) : ModelGateway {
    private val idCounter = AtomicInteger(INITIAL_MODEL_ID)

    private val _models = MutableStateFlow(MockFixtures.initialModels())
    override val models: StateFlow<ImmutableList<ModelDescriptor>> = _models.asStateFlow()

    override suspend fun setDefaultModel(modelId: String) {
        _models.update { list ->
            list.map { it.copy(isDefault = it.id == modelId) }.toImmutableList()
        }
    }

    override suspend fun addModel(
        displayName: String,
        provider: String,
        contextWindowLabel: String,
        capabilities: Set<ModelCapability>,
    ) {
        val trimmedName = displayName.trim()
        if (trimmedName.isEmpty()) return
        val descriptor =
            ModelDescriptor(
                id = "model-${idCounter.incrementAndGet()}",
                displayName = trimmedName,
                provider = provider.trim().ifEmpty { "Custom" },
                contextWindowLabel = contextWindowLabel.trim().ifEmpty { "128K" },
                hostNodeName = hostNameProvider(),
                capabilities = capabilities.toImmutableSet(),
            )
        _models.update { (it + descriptor).toImmutableList() }
    }

    override suspend fun removeModel(modelId: String) {
        _models.update { list ->
            val filtered = list.filterNot { it.id == modelId }
            if (filtered.none { it.isDefault } && filtered.isNotEmpty()) {
                filtered.mapIndexed { idx, item -> item.copy(isDefault = idx == 0) }.toImmutableList()
            } else {
                filtered.toImmutableList()
            }
        }
    }

    private companion object {
        const val INITIAL_MODEL_ID = 200
    }
}

class InMemoryAppSettingsStore : AppSettingsStore {
    private val _preferences = MutableStateFlow(AppPreferences())
    override val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    override fun updateThemeMode(mode: ThemeMode) {
        _preferences.update { it.copy(themeMode = mode) }
    }

    override fun updateThemeColorMode(mode: ThemeColorMode) {
        _preferences.update { it.copy(themeColorMode = mode) }
    }

    override fun updateSeedColor(preset: SeedColorPreset) {
        _preferences.update { it.copy(seedColor = preset) }
    }

    override fun updateCustomColorHex(hex: String) {
        _preferences.update { it.copy(customColorHex = hex) }
    }

    override fun updateLanguage(language: AppLanguage) {
        _preferences.update { it.copy(language = language) }
    }

    override fun updateChatTextSize(size: ChatTextSize) {
        _preferences.update { it.copy(chatTextSize = size) }
    }

    override fun updateAutoCollapseLongMessages(enabled: Boolean) {
        _preferences.update { it.copy(autoCollapseLongMessages = enabled) }
    }

    override fun updateExpandRunningActionTraces(enabled: Boolean) {
        _preferences.update { it.copy(expandRunningActionTraces = enabled) }
    }

    override fun updateExpandCompletedActionTraces(enabled: Boolean) {
        _preferences.update { it.copy(expandCompletedActionTraces = enabled) }
    }
}

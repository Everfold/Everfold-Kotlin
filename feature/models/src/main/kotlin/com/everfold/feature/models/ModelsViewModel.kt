package com.everfold.feature.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.ModelGateway
import com.everfold.core.model.ModelCapability
import com.everfold.core.model.ModelDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ModelsViewModel(
    private val modelGateway: ModelGateway,
) : ViewModel() {
    val models: StateFlow<ImmutableList<ModelDescriptor>> =
        modelGateway.models.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = persistentListOf(),
        )

    fun setDefault(modelId: String) {
        viewModelScope.launch {
            modelGateway.setDefaultModel(modelId)
        }
    }

    fun addModel(
        displayName: String,
        provider: String,
        contextWindowLabel: String,
    ) {
        viewModelScope.launch {
            modelGateway.addModel(
                displayName = displayName,
                provider = provider,
                contextWindowLabel = contextWindowLabel,
                capabilities = setOf(ModelCapability.REASONING, ModelCapability.TOOL_USE),
            )
        }
    }

    fun removeModel(modelId: String) {
        viewModelScope.launch {
            modelGateway.removeModel(modelId)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

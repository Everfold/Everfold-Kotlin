package com.everfold.core.gateway

import com.everfold.core.model.ModelCapability
import com.everfold.core.model.ModelDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

interface ModelGateway {
    val models: StateFlow<ImmutableList<ModelDescriptor>>

    suspend fun setDefaultModel(modelId: String)

    suspend fun addModel(
        displayName: String,
        provider: String,
        contextWindowLabel: String,
        capabilities: Set<ModelCapability>,
    )

    suspend fun removeModel(modelId: String)
}

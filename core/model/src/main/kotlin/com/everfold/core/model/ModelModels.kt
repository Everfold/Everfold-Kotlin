package com.everfold.core.model

import kotlinx.collections.immutable.ImmutableSet
import kotlinx.serialization.Serializable

@Serializable
enum class ModelCapability {
    REASONING,
    TOOL_USE,
    VISION,
    CODE_MODE,
}

data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val provider: String,
    val contextWindowLabel: String,
    val hostNodeName: String,
    val capabilities: ImmutableSet<ModelCapability>,
    val isDefault: Boolean = false,
)

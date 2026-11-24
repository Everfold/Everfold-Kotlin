package com.everfold.core.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
enum class ModelCapability {
    REASONING,
    TOOL_USE,
    VISION,
    CODE_MODE,
}

@Serializable
enum class ReasoningEffort {
    LOW,
    MEDIUM,
    HIGH,
    MAX,
}

data class ModelDescriptor(
    val id: String,
    val displayName: String,
    val provider: String,
    val contextWindowLabel: String,
    val hostNodeName: String,
    val capabilities: ImmutableSet<ModelCapability>,
    val supportedEfforts: ImmutableList<ReasoningEffort> = persistentListOf(),
    val defaultEffort: ReasoningEffort? = null,
    val isDefault: Boolean = false,
) {
    val supportsEffort: Boolean
        get() = supportedEfforts.isNotEmpty()
}

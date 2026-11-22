package com.everfold.core.gateway

import com.everfold.core.model.NodeDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.StateFlow

interface TopologyGateway {
    val nodes: StateFlow<ImmutableList<NodeDescriptor>>

    suspend fun refreshNodes()
}

package com.everfold.feature.nodes

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val nodesModule =
    module {
        viewModelOf(::NodesViewModel)
        viewModelOf(::NodeEditorViewModel)
    }

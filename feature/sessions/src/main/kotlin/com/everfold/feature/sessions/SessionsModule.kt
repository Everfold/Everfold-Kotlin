package com.everfold.feature.sessions

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sessionsModule =
    module {
        viewModelOf(::SessionDetailViewModel)
    }

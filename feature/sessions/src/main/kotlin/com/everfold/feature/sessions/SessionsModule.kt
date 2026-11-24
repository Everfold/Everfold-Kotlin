package com.everfold.feature.sessions

import com.everfold.feature.sessions.archived.ArchivedSessionsViewModel
import com.everfold.feature.sessions.search.SessionSearchViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sessionsModule =
    module {
        viewModelOf(::SessionDetailViewModel)
        viewModelOf(::SessionSearchViewModel)
        viewModelOf(::ArchivedSessionsViewModel)
    }

package com.everfold.core.gateway.mock

import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.gateway.ModelGateway
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.gateway.TopologyGateway
import org.koin.dsl.binds
import org.koin.dsl.module

val mockGatewayModule =
    module {
        single<AppSettingsStore> { InMemoryAppSettingsStore() }

        single {
            lateinit var sessions: MockSessionGateway
            val authority =
                MockAuthorityGateway(
                    onConnected = { mode -> sessions.seedIfEmpty(mode) },
                    onDisconnected = { sessions.clearAll() },
                )
            val models = MockModelGateway(hostNameProvider = authority::currentLoopHostName)
            sessions =
                MockSessionGateway(
                    modeProvider = authority::currentMode,
                    loopHostProvider = authority::currentLoopHostName,
                    defaultModelIdProvider = {
                        models.models.value
                            .firstOrNull { it.isDefault }
                            ?.id
                            .orEmpty()
                    },
                )
            MockGatewayContainer(authority = authority, sessions = sessions, models = models)
        }

        single { get<MockGatewayContainer>().authority } binds
            arrayOf(AuthorityGateway::class, TopologyGateway::class)
        single<SessionGateway> { get<MockGatewayContainer>().sessions }
        single<ModelGateway> { get<MockGatewayContainer>().models }
    }

internal data class MockGatewayContainer(
    val authority: MockAuthorityGateway,
    val sessions: MockSessionGateway,
    val models: MockModelGateway,
)

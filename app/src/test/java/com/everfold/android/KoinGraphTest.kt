package com.everfold.android

import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.gateway.ModelGateway
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.gateway.TopologyGateway
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.koin.dsl.koinApplication

class KoinGraphTest {
    @Test
    fun appModulesResolveAllGatewayContracts() {
        val koinApp =
            koinApplication {
                modules(everfoldAppModules())
            }
        try {
            val koin = koinApp.koin
            assertNotNull(koin.get<AuthorityGateway>())
            assertNotNull(koin.get<SessionGateway>())
            assertNotNull(koin.get<TopologyGateway>())
            assertNotNull(koin.get<ModelGateway>())
            assertNotNull(koin.get<AppSettingsStore>())
        } finally {
            koinApp.close()
        }
    }
}

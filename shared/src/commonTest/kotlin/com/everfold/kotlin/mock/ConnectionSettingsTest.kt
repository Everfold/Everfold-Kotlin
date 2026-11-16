package com.everfold.kotlin.mock

import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.connections.ClusterAuth
import com.everfold.kotlin.feature.connections.ClusterMethod
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionFailure
import com.everfold.kotlin.feature.connections.ConnectionPreviewResult
import com.everfold.kotlin.feature.connections.ConnectionStatus
import com.everfold.kotlin.ui.localization.UiText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionSettingsTest {
    @Test
    fun savingSettingsPreservesTheDefaultNameUntilExplicitlyRenamed() =
        runTest {
            val unavailable = mutableListOf<String>()
            val connections = MockConnections(this, unavailable::add)
            val initial = assertNotNull(connections.find(BUILT_IN_CONNECTION_ID))

            assertTrue(connections.save(initial.id, null, null, false))
            val saved = assertNotNull(connections.find(initial.id))
            assertEquals(initial.profile.name, saved.profile.name)
            assertFalse(saved.profile.autoConnect)
            assertEquals(initial.runtime, saved.runtime)

            assertFalse(connections.save(initial.id, "  ", null, true))
            assertEquals(saved, connections.find(initial.id))
            assertTrue(connections.save(initial.id, "  My device  ", null, true))
            assertTrue(connections.save(initial.id, null, null, false))
            assertEquals(UiText.Literal("My device"), connections.find(initial.id)?.profile?.name)
            assertTrue(unavailable.isEmpty())
        }

    @Test
    fun savedConnectionsStayOfflineAndBuiltInConnectionStaysFirst() =
        runTest {
            val connections = MockConnections(this) { }
            val first = assertNotNull(connections.add("Office", ClusterSetup()))
            val second = assertNotNull(connections.add("Lab", ClusterSetup()))
            connections.remove(BUILT_IN_CONNECTION_ID)
            assertEquals(listOf(BUILT_IN_CONNECTION_ID, first, second), connections.connections.map { it.id })
            assertEquals(ConnectionStatus.Disconnected, connections.find(first)?.runtime?.status)
            assertTrue(connections.save(BUILT_IN_CONNECTION_ID, "My device", ClusterSetup(), false))
            assertNull(connections.find(BUILT_IN_CONNECTION_ID)?.profile?.setup)
            assertNull(connections.add("  ", ClusterSetup()))
            assertNull(connections.add("Invalid", ClusterSetup(port = "bad")))
        }

    @Test
    fun concurrentAttemptsHaveIndependentResultsAndRetryDoesNotDisturbOthers() =
        runTest {
            val connections = MockConnections(this) { }
            val first = assertNotNull(connections.add("Office", ClusterSetup()))
            val second = assertNotNull(connections.add("Lab", ClusterSetup()))
            connections.connect(first)
            connections.connect(second, ConnectionPreviewResult.Credentials)
            assertEquals(ConnectionStatus.Connecting, connections.find(first)?.runtime?.status)
            advanceUntilIdle()
            assertTrue(connections.find(first)?.isConnected == true)
            assertTrue(connections.find(BUILT_IN_CONNECTION_ID)?.isConnected == true)
            assertEquals(ConnectionStatus.Failed, connections.find(second)?.runtime?.status)
            assertEquals(ConnectionFailure.Credentials, connections.find(second)?.runtime?.failure)
            connections.connect(second)
            advanceUntilIdle()
            assertTrue(connections.connections.all { it.isConnected })
            assertNull(connections.find(second)?.runtime?.failure)
            assertEquals(listOf(BUILT_IN_CONNECTION_ID, first, second), connections.connections.map { it.id })
        }

    @Test
    fun cancelledAndRemovedAttemptsCannotRestoreConnections() =
        runTest {
            val connections = MockConnections(this) { }
            val first = assertNotNull(connections.add("Office", ClusterSetup()))
            val second = assertNotNull(connections.add("Lab", ClusterSetup()))
            connections.connect(first)
            connections.connect(second)
            advanceTimeBy(400)
            connections.disconnect(first)
            connections.remove(second)
            advanceUntilIdle()
            assertEquals(ConnectionStatus.Disconnected, connections.find(first)?.runtime?.status)
            assertNull(connections.find(second))
            connections.connect(first, ConnectionPreviewResult.Timeout)
            advanceTimeBy(400)
            connections.disconnect(first)
            connections.connect(first)
            advanceUntilIdle()
            assertTrue(connections.find(first)?.isConnected == true)
            assertNull(connections.find(first)?.runtime?.failure)
        }

    @Test
    fun savingConnectionParametersCancelsOnlyItsAttemptAndRetainsCredentials() =
        runTest {
            val unavailable = mutableListOf<String>()
            val connections = MockConnections(this, unavailable::add)
            val setup =
                ClusterSetup(
                    method = ClusterMethod.Central,
                    ip = "192.168.1.1",
                    port = "443",
                    auth = ClusterAuth.Mtls,
                    caCertificate = "ca.pem",
                    clientPublicKey = "public.pem",
                    clientPrivateKey = "private.pem",
                )
            val first = assertNotNull(connections.add("Office", setup))
            val second = assertNotNull(connections.add("Lab", ClusterSetup()))
            connections.connect(first)
            connections.connect(second)
            advanceTimeBy(400)
            assertTrue(connections.save(first, "Office", setup.copy(port = "8443"), true))
            advanceUntilIdle()
            assertEquals(ConnectionStatus.Disconnected, connections.find(first)?.runtime?.status)
            assertEquals(setup.copy(port = "8443"), connections.find(first)?.profile?.setup)
            assertTrue(connections.find(first)?.profile?.autoConnect == true)
            assertTrue(connections.find(second)?.isConnected == true)
            assertEquals(listOf(first), unavailable)
            assertFalse(connections.save(second, " ", ClusterSetup(), false))
            assertTrue(connections.save(second, "Renamed", ClusterSetup(), false))
            assertTrue(connections.find(second)?.isConnected == true)
        }

    @Test
    fun disconnectRetainsTheLastSuccessfulInformation() =
        runTest {
            val connections = MockConnections(this) { }
            val id = assertNotNull(connections.add("Office", ClusterSetup()))
            connections.connect(id)
            advanceUntilIdle()
            val lastConnected = assertNotNull(connections.find(id)?.runtime?.lastConnectedAt)
            connections.disconnect(id)
            assertEquals(ConnectionStatus.Disconnecting, connections.find(id)?.runtime?.status)
            advanceUntilIdle()
            assertEquals(ConnectionStatus.Disconnected, connections.find(id)?.runtime?.status)
            assertEquals(lastConnected, connections.find(id)?.runtime?.lastConnectedAt)
            assertNotNull(connections.find(id)?.runtime?.kernelVersion)
        }
}

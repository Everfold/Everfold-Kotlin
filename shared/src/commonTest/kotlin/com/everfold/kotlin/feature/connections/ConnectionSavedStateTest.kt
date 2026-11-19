package com.everfold.kotlin.feature.connections

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConnectionSavedStateTest {
    @Test
    fun draftRestorationPreservesConnectionSettingsWithoutPersistingSecrets() {
        for (method in ClusterMethod.entries) {
            for (auth in ClusterAuth.entries) {
                val setup =
                    ClusterSetup(
                        method = method,
                        ip = "2001:db8::1",
                        port = "8443",
                        auth = auth,
                        account = "operator",
                        password = "example-password",
                        serverCertificate = "server.pem",
                        caCertificate = "ca.pem",
                        clientPublicKey = "public.pem",
                        clientPrivateKey = "private.pem",
                    )
                val draft = ConnectionDraft("  Office  ", setup, true)
                val expected = draft.copy(setup = setup.copy(password = "", clientPrivateKey = null))
                val actual = restored(ConnectionDraft.Saver, draft)
                assertEquals(expected, actual, "$method / $auth")
                assertEquals("", actual.setup?.password)
                assertNull(actual.setup?.clientPrivateKey)
            }
        }
    }

    @Test
    fun anUneditedBuiltInNameRemainsUnchangedAfterRestoration() {
        val draft = ConnectionDraft(null, null, false)
        val restored = restored(ConnectionDraft.Saver, draft)
        assertEquals(draft, restored)
        assertNull(restored.name)
        assertTrue(restored.isValid)
    }

    @Test
    fun incompleteDraftsKeepTheirInputAndValidationAfterRestoration() {
        val draft = ConnectionDraft("Office", ClusterSetup(ip = "192.168."), false)
        val restored = restored(ConnectionDraft.Saver, draft)
        assertEquals(draft, restored)
        assertFalse(restored.isValid)
        val unnamed = ConnectionDraft("  ", null, true)
        assertFalse(restored(ConnectionDraft.Saver, unnamed).isValid)
    }

    @Test
    fun restoredNavigationReturnsFromAddingToThePreviousConnectionThenTheList() {
        val navigation = ConnectionNavigationState()
        navigation.open("office")
        navigation.add()
        val restored = restored(ConnectionNavigationState.Saver, navigation)
        assertTrue(restored.adding)
        assertEquals("office", restored.selectedId)
        assertTrue(restored.back())
        assertFalse(restored.adding)
        assertEquals("office", restored.selectedId)
        assertTrue(restored.back())
        assertNull(restored.selectedId)
        assertFalse(restored.back())
        assertFalse(restored(ConnectionNavigationState.Saver, restored).canGoBack)
    }

    private fun <T : Any> restored(
        saver: Saver<T, Any>,
        value: T,
    ): T {
        val scope =
            object : SaverScope {
                override fun canBeSaved(value: Any) = value is String || value is Boolean
            }
        val saved = with(saver) { scope.save(value) }
        return assertNotNull(saver.restore(assertNotNull(saved)))
    }
}

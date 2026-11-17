package com.everfold.kotlin.preferences

import com.everfold.kotlin.feature.settings.ConversationTextSize
import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmPreferenceStoreTest {
    @Test
    fun preferencesCanBeReadThroughANewStoreAfterFlushing() {
        val path = "com/everfold/tests/${UUID.randomUUID()}"
        val node = Preferences.userRoot().node(path)
        try {
            val first = UserPreferencesState(JvmPreferenceStore(node))
            first.update { it.copy(textSize = ConversationTextSize.ExtraLarge, sideMargins = false) }
            val reopened = Preferences.userRoot().node(path)
            reopened.sync()
            assertEquals(first.value, UserPreferencesState(JvmPreferenceStore(reopened)).value)
        } finally {
            node.removeNode()
            Preferences.userRoot().flush()
        }
    }
}

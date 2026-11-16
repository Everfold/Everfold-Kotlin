package com.everfold.kotlin.feature.connections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue

/** 列表选择只影响导航不改变任何连接或会话的归属 */
internal class ConnectionNavigationState(
    selectedId: String? = null,
    adding: Boolean = false,
) {
    var selectedId by mutableStateOf(selectedId)
        private set
    var adding by mutableStateOf(adding)
        private set

    val canGoBack: Boolean get() = adding || selectedId != null

    fun open(id: String) {
        selectedId = id
        adding = false
    }

    fun add() {
        adding = true
    }

    fun showList() {
        selectedId = null
        adding = false
    }

    fun back(): Boolean =
        when {
            adding -> {
                adding = false
                true
            }

            selectedId != null -> {
                selectedId = null
                true
            }

            else -> {
                false
            }
        }

    companion object {
        val Saver =
            listSaver<ConnectionNavigationState, Any>(
                save = { listOf(it.selectedId.orEmpty(), it.adding) },
                restore = { ConnectionNavigationState((it[0] as String).ifEmpty { null }, it[1] as Boolean) },
            )
    }
}

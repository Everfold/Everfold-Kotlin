package com.everfold.kotlin.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.Connection
import com.everfold.kotlin.feature.connections.ConnectionManager
import com.everfold.kotlin.feature.connections.ConnectionPreviewResult
import com.everfold.kotlin.feature.connections.ConnectionProfile
import com.everfold.kotlin.feature.connections.ConnectionRuntimeState
import com.everfold.kotlin.feature.connections.ConnectionStatus
import com.everfold.kotlin.feature.connections.failure
import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_this_device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

private const val CONNECT_DELAY_MILLIS = 900L
private const val DISCONNECT_DELAY_MILLIS = 250L
private val BusyConnectionStatuses =
    setOf(ConnectionStatus.Connected, ConnectionStatus.Connecting, ConnectionStatus.Disconnecting)

/** 连接任务随工作区存活每个 ID 独立取消模拟结果不会跨连接或配置覆盖 */
internal class MockConnections(
    private val scope: CoroutineScope,
    private val onUnavailable: (String) -> Unit,
) : ConnectionManager {
    override var connections by mutableStateOf(
        listOf(
            Connection(
                ConnectionProfile(BUILT_IN_CONNECTION_ID, UiText.Resource(Res.string.connection_this_device), autoConnect = true),
                ConnectionRuntimeState(
                    ConnectionStatus.Connected,
                    lastConnectedAt = Clock.System.now(),
                    kernelVersion = "Beamlet · preview",
                ),
            ),
        ),
    )
        private set

    private var nextId = 0
    private val operations = mutableMapOf<String, Job>()

    override fun add(
        name: String,
        setup: ClusterSetup,
        autoConnect: Boolean,
    ): String? {
        if (name.isBlank() || !setup.isValid) return null
        val id = "connection-${++nextId}"
        connections = connections + Connection(ConnectionProfile(id, UiText.Literal(name.trim()), setup, autoConnect))
        return id
    }

    override fun save(
        id: String,
        name: String?,
        setup: ClusterSetup?,
        autoConnect: Boolean,
    ): Boolean {
        val connection = find(id)
        val invalidEndpoint = connection?.profile?.canEditEndpoint == true && (setup == null || !setup.isValid)
        if (connection == null || name?.isBlank() == true || invalidEndpoint) return false
        val endpoint = if (connection.profile.canEditEndpoint) setup else null
        if (endpoint != connection.profile.setup) {
            cancelOperation(id)
            onUnavailable(id)
            update(id) { it.copy(runtime = ConnectionRuntimeState()) }
        }
        val savedName = name?.let { UiText.Literal(it.trim()) } ?: connection.profile.name
        update(id) { it.copy(profile = it.profile.copy(name = savedName, setup = endpoint, autoConnect = autoConnect)) }
        return true
    }

    private var previewResults by mutableStateOf(emptyMap<String, ConnectionPreviewResult>())

    fun previewResult(id: String): ConnectionPreviewResult = previewResults[id] ?: ConnectionPreviewResult.Success

    fun selectPreviewResult(
        id: String,
        result: ConnectionPreviewResult,
    ) {
        if (find(id) != null) previewResults = previewResults + (id to result)
    }

    override fun connect(id: String) = connect(id, previewResult(id))

    fun connect(
        id: String,
        result: ConnectionPreviewResult,
    ) {
        val connection = find(id) ?: return
        if (connection.runtime.status in BusyConnectionStatuses) {
            return
        }
        cancelOperation(id)
        update(id) { it.copy(runtime = it.runtime.copy(status = ConnectionStatus.Connecting, failure = null)) }
        val job =
            scope.launch {
                delay(CONNECT_DELAY_MILLIS)
                update(id) {
                    it.copy(
                        runtime =
                            if (result == ConnectionPreviewResult.Success) {
                                ConnectionRuntimeState(
                                    ConnectionStatus.Connected,
                                    lastConnectedAt = Clock.System.now(),
                                    kernelVersion = "Beamlet · preview",
                                )
                            } else {
                                it.runtime.copy(status = ConnectionStatus.Failed, failure = result.failure)
                            },
                    )
                }
            }
        operations[id] = job
        job.invokeOnCompletion { if (operations[id] === job) operations.remove(id) }
    }

    override fun disconnect(id: String) {
        val connection = find(id) ?: return
        cancelOperation(id)
        onUnavailable(id)
        if (!connection.isConnected) {
            update(id) { it.copy(runtime = it.runtime.copy(status = ConnectionStatus.Disconnected, failure = null)) }
            return
        }
        update(id) { it.copy(runtime = it.runtime.copy(status = ConnectionStatus.Disconnecting)) }
        val job =
            scope.launch {
                delay(DISCONNECT_DELAY_MILLIS)
                update(id) { it.copy(runtime = it.runtime.copy(status = ConnectionStatus.Disconnected, failure = null)) }
            }
        operations[id] = job
        job.invokeOnCompletion { if (operations[id] === job) operations.remove(id) }
    }

    override fun remove(id: String) {
        if (find(id)?.profile?.canRemove != true) return
        cancelOperation(id)
        onUnavailable(id)
        connections = connections.filterNot { it.id == id }
        previewResults = previewResults - id
    }

    private fun cancelOperation(id: String) {
        operations.remove(id)?.cancel()
    }

    private fun update(
        id: String,
        transform: (Connection) -> Connection,
    ) {
        connections = connections.map { if (it.id == id) transform(it) else it }
    }
}

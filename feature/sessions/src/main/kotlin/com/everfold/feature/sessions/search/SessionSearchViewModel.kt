package com.everfold.feature.sessions.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.gateway.TopologyGateway
import com.everfold.core.model.LoopState
import com.everfold.core.model.SessionSearchFilter
import com.everfold.core.model.SessionSearchHit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SessionSearchUiState(
    val query: String = "",
    val filter: SessionSearchFilter = SessionSearchFilter(),
    val availableWorkspaces: ImmutableList<String> = persistentListOf(),
    val availableNodes: ImmutableList<String> = persistentListOf(),
    val hits: ImmutableList<SessionSearchHit> = persistentListOf(),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SessionSearchViewModel(
    private val sessionGateway: SessionGateway,
    topologyGateway: TopologyGateway,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(SessionSearchFilter())

    private val debouncedQuery =
        queryFlow.debounce { text ->
            if (text.isBlank()) 0L else SEARCH_DEBOUNCE_MS
        }

    private val searchHits =
        combine(debouncedQuery, filterFlow) { query, filter -> query to filter }
            .flatMapLatest { (query, filter) ->
                sessionGateway.searchSessions(query = query, filter = filter)
            }

    private val filterOptions =
        combine(sessionGateway.sessions, topologyGateway.nodes) { sessions, nodes ->
            val workspaces =
                sessions
                    .map { it.workspaceName }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .toImmutableList()
            val nodeNames =
                (nodes.map { it.name } + sessions.map { it.loopHostNodeName })
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .toImmutableList()
            workspaces to nodeNames
        }

    val uiState: StateFlow<SessionSearchUiState> =
        combine(
            queryFlow,
            filterFlow,
            filterOptions,
            searchHits,
        ) { query, filter, (workspaces, nodes), hits ->
            SessionSearchUiState(
                query = query,
                filter = filter,
                availableWorkspaces = workspaces,
                availableNodes = nodes,
                hits = hits,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SessionSearchUiState(),
        )

    fun updateQuery(value: String) {
        queryFlow.value = value
    }

    fun clearQuery() {
        queryFlow.value = ""
    }

    fun selectWorkspace(workspace: String?) {
        filterFlow.update { it.copy(workspaceName = workspace) }
    }

    fun selectNode(node: String?) {
        filterFlow.update { it.copy(nodeName = node) }
    }

    fun selectLoopState(state: LoopState?) {
        filterFlow.update { it.copy(loopState = state) }
    }

    fun toggleIncludeArchived() {
        filterFlow.update { it.copy(includeArchived = !it.includeArchived) }
    }

    fun clearFilters() {
        filterFlow.value = SessionSearchFilter()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 150L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

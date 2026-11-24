package com.everfold.core.gateway.mock

import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.ObservedItem
import com.everfold.core.model.SessionMatchSource
import com.everfold.core.model.SessionSearchFilter
import com.everfold.core.model.SessionSearchHit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val SNIPPET_BEFORE_LEN = 24
private const val SNIPPET_AFTER_LEN = 60

internal fun filterAndSearchSessions(
    sessions: Collection<AgentSessionDetail>,
    query: String,
    filter: SessionSearchFilter,
): ImmutableList<SessionSearchHit> {
    val trimmedQuery = query.trim()
    return sessions
        .asSequence()
        .filter { it.matchesFilter(filter) }
        .mapNotNull { detail ->
            if (trimmedQuery.isEmpty()) {
                SessionSearchHit(summary = detail.summary)
            } else {
                detail.searchByTitleAndMessages(trimmedQuery)
            }
        }.toImmutableList()
}

private fun AgentSessionDetail.matchesFilter(filter: SessionSearchFilter): Boolean {
    val archiveOk = filter.includeArchived || !summary.isArchived
    val workspaceOk =
        filter.workspaceName == null || summary.workspaceName == filter.workspaceName
    val stateOk = filter.loopState == null || summary.loopState == filter.loopState
    val nodeOk = filter.nodeName == null || matchesNode(filter.nodeName!!)
    return archiveOk && workspaceOk && stateOk && nodeOk
}

private fun AgentSessionDetail.matchesNode(nodeName: String): Boolean {
    if (summary.loopHostNodeName == nodeName) return true
    return items
        .asSequence()
        .filterIsInstance<ObservedItem.ActionGroup>()
        .flatMap { it.actions.asSequence() }
        .any { it.executedOn?.nodeName == nodeName }
}

private fun AgentSessionDetail.searchByTitleAndMessages(query: String): SessionSearchHit? {
    val titleMatched = summary.title.contains(query, ignoreCase = true)
    val matchedMessage = findMatchedMessage(query)
    if (!titleMatched && matchedMessage == null) return null

    val source = if (titleMatched) SessionMatchSource.TITLE else SessionMatchSource.MESSAGE
    return SessionSearchHit(
        summary = summary,
        matchedSnippet = matchedMessage?.second,
        matchedItemId = matchedMessage?.first,
        matchSource = source,
    )
}

private fun AgentSessionDetail.findMatchedMessage(query: String): Pair<String, String>? {
    for (item in items.asReversed()) {
        val text =
            when (item) {
                is ObservedItem.UserMessage -> item.content
                is ObservedItem.AssistantMessage -> item.markdown
                is ObservedItem.ActionGroup -> null
            }
        if (text != null && text.contains(query, ignoreCase = true)) {
            return item.id to extractSnippet(text = text, query = query)
        }
    }
    return null
}

private fun extractSnippet(
    text: String,
    query: String,
): String {
    val normalized = text.replace(Regex("\\s+"), " ").trim()
    val matchIndex = normalized.indexOf(query, ignoreCase = true)
    if (matchIndex < 0) return normalized.take(SNIPPET_BEFORE_LEN + SNIPPET_AFTER_LEN)

    val start = (matchIndex - SNIPPET_BEFORE_LEN).coerceAtLeast(0)
    val end = (matchIndex + query.length + SNIPPET_AFTER_LEN).coerceAtMost(normalized.length)
    val prefix = if (start > 0) "…" else ""
    val suffix = if (end < normalized.length) "…" else ""
    return "$prefix${normalized.substring(start, end)}$suffix"
}

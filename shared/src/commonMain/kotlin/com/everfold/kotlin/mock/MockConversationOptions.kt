package com.everfold.kotlin.mock

import com.everfold.kotlin.feature.connections.ConnectionManager
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.feature.conversation.AttachmentTarget
import com.everfold.kotlin.feature.conversation.ConversationOptions
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.DRAFT_CONVERSATION_ID
import kotlinx.coroutines.CoroutineScope

internal class MockConversationOptions(
    scope: CoroutineScope,
    private val connections: ConnectionManager,
    private val read: (String) -> ConversationUiState?,
    private val update: (String, (ConversationUiState) -> ConversationUiState) -> Unit,
) : ConversationOptions {
    val attachments = MockAttachments(scope, read, update) { connections.find(it)?.isConnected == true }

    override fun workspaces(connectionId: String) =
        if (connections.find(connectionId) != null) MockConversationCatalog.workspaces(connectionId) else emptyList()

    override fun models(connectionId: String) =
        if (connections.find(connectionId) != null) MockConversationCatalog.models(connectionId) else emptyList()

    override fun selectWorkspace(
        conversationId: String,
        workspaceId: String,
    ) {
        val state = read(conversationId) ?: return
        val selected = workspaces(state.connectionId).find { it.id == workspaceId }
        if (conversationId == DRAFT_CONVERSATION_ID && selected != null && selected != state.workspace) {
            attachments.interrupt(conversationId)
            update(
                conversationId,
            ) { it.copy(workspace = selected, attachments = emptyList(), attachmentRevision = it.attachmentRevision + 1) }
        }
    }

    override fun selectModel(
        conversationId: String,
        modelId: String,
    ) {
        val state = read(conversationId) ?: return
        val selected = models(state.connectionId).find { it.id == modelId }
        if (!state.isResponding && selected != null) update(conversationId) { it.copy(model = selected) }
    }

    override fun attach(
        target: AttachmentTarget,
        files: List<AttachmentSource>,
    ) = attachments.attach(target, files)

    override fun retryAttachment(
        conversationId: String,
        attachmentId: String,
    ) = attachments.retry(conversationId, attachmentId)

    override fun removeAttachment(
        conversationId: String,
        attachmentId: String,
    ) = attachments.remove(conversationId, attachmentId)
}

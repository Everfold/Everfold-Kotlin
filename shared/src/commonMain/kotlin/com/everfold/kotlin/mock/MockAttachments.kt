package com.everfold.kotlin.mock

import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.feature.conversation.AttachmentStatus
import com.everfold.kotlin.feature.conversation.AttachmentTarget
import com.everfold.kotlin.feature.conversation.ConversationAttachment
import com.everfold.kotlin.feature.conversation.ConversationUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val UPLOAD_STEP_MILLIS = 180L
private const val UPLOAD_STEPS = 5

/** 模拟传输任务与会话共同存活，切页不取消，切换目标或删除会取消。 */
internal class MockAttachments(
    private val scope: CoroutineScope,
    private val read: (String) -> ConversationUiState?,
    private val update: (String, (ConversationUiState) -> ConversationUiState) -> Unit,
    private val connected: (String) -> Boolean,
) {
    private var nextId = 0
    private val uploads = mutableMapOf<Pair<String, String>, Job>()

    fun attach(
        target: AttachmentTarget,
        files: List<AttachmentSource>,
    ) {
        val state = read(target.conversationId) ?: return
        if (state.attachmentTarget != target || state.isResponding || state.workspace == null) return
        val existing = state.attachments.map { it.source.reference }.toSet()
        val added =
            files
                .distinctBy { it.reference }
                .filter { it.reference !in existing && it.name.isNotBlank() }
                .map { ConversationAttachment("attachment-${++nextId}", it) }
        update(state.id) { it.copy(attachments = it.attachments + added) }
        added.forEach { retry(state.id, it.id) }
    }

    fun retry(
        conversationId: String,
        attachmentId: String,
    ) {
        val state = read(conversationId) ?: return
        if (state.isResponding || state.attachments.none { it.id == attachmentId }) return
        val key = conversationId to attachmentId
        uploads.remove(key)?.cancel()
        change(conversationId, attachmentId) { it.copy(status = AttachmentStatus.Uploading, progress = 0f) }
        uploads[key] =
            scope.launch {
                repeat(UPLOAD_STEPS) { step ->
                    delay(UPLOAD_STEP_MILLIS)
                    if (!connected(state.connectionId)) {
                        change(conversationId, attachmentId) { it.copy(status = AttachmentStatus.Failed) }
                        uploads.remove(key)
                        return@launch
                    }
                    change(conversationId, attachmentId) { it.copy(progress = (step + 1).toFloat() / UPLOAD_STEPS) }
                }
                change(conversationId, attachmentId) { it.copy(status = AttachmentStatus.Ready) }
                uploads.remove(key)
            }
    }

    fun remove(
        conversationId: String,
        attachmentId: String,
    ) {
        uploads.remove(conversationId to attachmentId)?.cancel()
        update(conversationId) { it.copy(attachments = it.attachments.filterNot { file -> file.id == attachmentId }) }
    }

    fun interrupt(conversationId: String) {
        uploads.keys.filter { it.first == conversationId }.forEach { uploads.remove(it)?.cancel() }
        update(conversationId) { state ->
            state.copy(
                attachments =
                    state.attachments.map {
                        if (it.status == AttachmentStatus.Uploading) it.copy(status = AttachmentStatus.Failed) else it
                    },
            )
        }
    }

    private fun change(
        id: String,
        attachmentId: String,
        transform: (ConversationAttachment) -> ConversationAttachment,
    ) {
        update(id) { state -> state.copy(attachments = state.attachments.map { if (it.id == attachmentId) transform(it) else it }) }
    }
}

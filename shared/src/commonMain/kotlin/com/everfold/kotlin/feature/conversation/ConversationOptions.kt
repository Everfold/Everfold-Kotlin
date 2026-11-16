package com.everfold.kotlin.feature.conversation

import com.everfold.kotlin.ui.localization.UiText

/** 连接提供的项目与模型快照，ID 仅在所属连接内使用。 */
data class SessionWorkspace(
    val id: String,
    val name: UiText,
    val path: String,
)

data class ConversationModel(
    val id: String,
    val name: UiText,
)

/** 文件选择结果只持有平台引用，读取与传输由后端实现负责。 */
data class AttachmentSource(
    val name: String,
    val reference: String,
)

enum class AttachmentStatus { Uploading, Ready, Failed }

data class ConversationAttachment(
    val id: String,
    val source: AttachmentSource,
    val status: AttachmentStatus = AttachmentStatus.Uploading,
    val progress: Float = 0f,
)

/** 捕获打开文件选择器时的目标，防止延迟结果进入已切换的草稿。 */
data class AttachmentTarget(
    val conversationId: String,
    val connectionId: String,
    val workspaceId: String?,
    val revision: Int,
)

internal interface ConversationOptions {
    fun workspaces(connectionId: String): List<SessionWorkspace>

    fun models(connectionId: String): List<ConversationModel>

    fun selectWorkspace(
        conversationId: String,
        workspaceId: String,
    )

    fun selectModel(
        conversationId: String,
        modelId: String,
    )

    fun attach(
        target: AttachmentTarget,
        files: List<AttachmentSource>,
    )

    fun retryAttachment(
        conversationId: String,
        attachmentId: String,
    )

    fun removeAttachment(
        conversationId: String,
        attachmentId: String,
    )
}

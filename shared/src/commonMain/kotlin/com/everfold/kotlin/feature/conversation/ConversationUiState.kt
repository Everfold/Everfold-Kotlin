package com.everfold.kotlin.feature.conversation

import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.ui.localization.UiText

internal const val DRAFT_CONVERSATION_ID = "new-conversation"

/** 消息角色区分用户输入与助手回复 */
enum class MessageRole { User, Assistant }

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
    val attachments: List<ConversationAttachment> = emptyList(),
    val model: ConversationModel? = null,
)

/** 会话界面的不可变快照资源文案到渲染时再解析保留动态语言切换能力 */
data class ConversationUiState(
    val id: String,
    val title: UiText,
    val preview: UiText,
    val updatedLabel: UiText,
    val connectionId: String = BUILT_IN_CONNECTION_ID,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val workspace: SessionWorkspace? = null,
    val model: ConversationModel? = null,
    val attachments: List<ConversationAttachment> = emptyList(),
    val attachmentRevision: Int = 0,
    val isPinned: Boolean = false,
    // 保留归档数据字段当前侧栏不按此字段隐藏会话
    val isArchived: Boolean = false,
    val runState: AgentRunState = AgentRunState.Idle,
) {
    val isResponding: Boolean get() = runState == AgentRunState.Running
    val responseStopped: Boolean get() = runState == AgentRunState.Stopped
    val hasSendableContent: Boolean get() =
        (draft.isNotBlank() || attachments.isNotEmpty()) &&
            attachments.all { it.status == AttachmentStatus.Ready } && workspace != null && model != null
    val attachmentTarget: AttachmentTarget get() = AttachmentTarget(id, connectionId, workspace?.id, attachmentRevision)
}

/** 界面执行状态中文标签统一定义在 AgentRunState.label */
enum class AgentRunState { Idle, Running, Waiting, Completed, Stopped, Failed }

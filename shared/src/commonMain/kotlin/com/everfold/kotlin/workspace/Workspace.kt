package com.everfold.kotlin.workspace

import com.everfold.kotlin.feature.connections.ConnectionManager
import com.everfold.kotlin.feature.conversation.ConversationOptions
import com.everfold.kotlin.feature.conversation.ConversationUiState

/**
 * 工作台的可观察状态与用户动作实现持有任务生命周期页面不负责生成回复。
 * 会话始终绑定创建时的连接连接不可用时拒绝发送并保留消息与草稿。
 */
internal interface Workspace {
    val connections: ConnectionManager
    val conversations: List<ConversationUiState>
    val newConversation: ConversationUiState
    val conversationOptions: ConversationOptions

    fun updateNewConversationDraft(value: String)

    /** 首次发送才创建正式会话无法发送时返回 null 且保留草稿 */
    fun submitNewConversation(): String?

    fun renameConversation(
        id: String,
        title: String,
    )

    fun togglePinned(id: String)

    fun deleteConversation(id: String)

    fun updateDraft(
        id: String,
        value: String,
    )

    fun selectNewConversationConnection(id: String)

    fun send(id: String)

    fun retry(id: String)

    fun stop(id: String)
}

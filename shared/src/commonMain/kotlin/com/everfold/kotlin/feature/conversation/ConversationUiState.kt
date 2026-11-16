package com.everfold.kotlin.feature.conversation

import com.everfold.kotlin.ui.localization.UiText

/** 消息角色区分用户输入与助手回复 */
enum class MessageRole { User, Assistant }

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val text: String,
)

/** 会话界面的不可变快照资源文案到渲染时再解析保留动态语言切换能力 */
data class ConversationUiState(
    val id: String,
    val title: UiText,
    val preview: UiText,
    val updatedLabel: UiText,
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val isPinned: Boolean = false,
    // 保留归档数据字段当前侧栏不按此字段隐藏会话
    val isArchived: Boolean = false,
    val runState: AgentRunState = AgentRunState.Idle,
) {
    val isResponding: Boolean get() = runState == AgentRunState.Running
    val responseStopped: Boolean get() = runState == AgentRunState.Stopped
}

/** 界面执行状态中文标签统一定义在 AgentRunState.label */
enum class AgentRunState { Idle, Running, Waiting, Completed, Stopped, Failed }

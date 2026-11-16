package com.everfold.kotlin.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.ChatMessage
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.DRAFT_CONVERSATION_ID
import com.everfold.kotlin.feature.conversation.MessageRole
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.theme.ThemeColorMode
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import com.everfold.kotlin.ui.theme.ThemePreset
import com.everfold.kotlin.workspace.Workspace
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.just_now
import everfold.shared.generated.resources.new_conversation_hint
import everfold.shared.generated.resources.new_session
import everfold.shared.generated.resources.reply_complete
import everfold.shared.generated.resources.reply_stopped
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 内存模拟工作区驱动界面演示不创建 Agent连接网络或持久化数据 */
internal class MockWorkspace :
    ViewModel(),
    Workspace {
    var initialized by mutableStateOf(false)
        private set
    override val connections = MockConnections(viewModelScope, ::stopConnectionSessions)

    fun completeSetup(setup: ClusterSetup?) {
        if (initialized) return
        if (setup != null) {
            val id = connections.add(setup.ip.ifBlank { "Beamlet" }, setup, autoConnect = true)
            if (id != null) {
                connections.connect(id)
                selectNewConversationConnection(id)
            }
        }
        initialized = true
    }

    override val conversationOptions = MockConversationOptions(viewModelScope, connections, ::read, ::update)

    override var conversations by mutableStateOf(sampleConversations().map(MockConversationCatalog::initialize))
        private set
    override var themeMode by mutableStateOf(ThemeMode.System)
        private set
    override var themeColor by mutableStateOf(ThemeColorSelection())
        private set
    var replyText: String = "Preview reply"

    private var nextId = 0
    private val replies = mutableMapOf<String, Job>()

    override var newConversation by mutableStateOf(
        MockConversationCatalog.initialize(
            ConversationUiState(
                id = DRAFT_CONVERSATION_ID,
                // 新建会话
                title = UiText.Resource(Res.string.new_session),
                // 等待输入
                preview = UiText.Resource(Res.string.new_conversation_hint),
                // 刚刚
                updatedLabel = UiText.Resource(Res.string.just_now),
            ),
        ),
    )
        private set

    override fun updateNewConversationDraft(value: String) {
        newConversation = newConversation.copy(draft = value)
    }

    /** 首次发送时把草稿转为正式会话置顶会话仍排在列表前方 */
    override fun submitNewConversation(): String? {
        val input = newConversation.draft.trim()
        if (!newConversation.hasSendableContent || connections.find(newConversation.connectionId)?.isConnected != true) return null
        val id = "new-${++nextId}"
        val session =
            newConversation.copy(
                id = id,
                title =
                    UiText.Literal(
                        input
                            .ifBlank {
                                newConversation.attachments
                                    .first()
                                    .source.name
                            }.take(24),
                    ),
            )
        conversations = (listOf(session) + conversations).sortedByDescending { it.isPinned }
        newConversation =
            newConversation.copy(draft = "", attachments = emptyList(), attachmentRevision = newConversation.attachmentRevision + 1)
        send(id)
        return id
    }

    override fun renameConversation(
        id: String,
        title: String,
    ) {
        if (title.isBlank()) return
        update(id) { it.copy(title = UiText.Literal(title.trim())) }
    }

    override fun togglePinned(id: String) {
        update(id) { it.copy(isPinned = !it.isPinned) }
        conversations = conversations.sortedByDescending { it.isPinned }
    }

    /** 保留归档状态切换能力当前界面不提供归档入口 */
    fun toggleArchived(id: String) {
        update(id) { it.copy(isArchived = !it.isArchived) }
    }

    override fun deleteConversation(id: String) {
        conversationOptions.attachments.interrupt(id)
        replies.remove(id)?.cancel()
        conversations = conversations.filterNot { it.id == id }
    }

    override fun updateDraft(
        id: String,
        value: String,
    ) = update(id) { it.copy(draft = value) }

    override fun setTheme(mode: ThemeMode) {
        themeMode = mode
    }

    override fun useSystemThemeColor() {
        themeColor = themeColor.copy(mode = ThemeColorMode.System)
    }

    override fun selectThemePreset(preset: ThemePreset) {
        themeColor = themeColor.copy(mode = ThemeColorMode.Preset, preset = preset)
    }

    override fun applyCustomThemeColor(color: Color) {
        themeColor = themeColor.copy(mode = ThemeColorMode.Custom, customSeed = color)
    }

    override fun selectNewConversationConnection(id: String) {
        if (connections.find(id) != null && id != newConversation.connectionId) {
            conversationOptions.attachments.interrupt(newConversation.id)
            newConversation =
                MockConversationCatalog.initialize(
                    newConversation.copy(
                        connectionId = id,
                        attachments = emptyList(),
                        attachmentRevision =
                            newConversation.attachmentRevision + 1,
                    ),
                )
        }
    }

    private fun stopConnectionSessions(connectionId: String) {
        (conversations + newConversation)
            .filter { it.connectionId == connectionId }
            .forEach { conversationOptions.attachments.interrupt(it.id) }
        conversations
            .filter { it.connectionId == connectionId && it.runState in listOf(AgentRunState.Running, AgentRunState.Waiting) }
            .forEach { stop(it.id) }
    }

    override fun send(id: String) {
        val session = conversations.firstOrNull { it.id == id } ?: return
        val input = session.draft.trim()
        if (!session.hasSendableContent || session.isResponding || connections.find(session.connectionId)?.isConnected != true) return
        val messageId = "message-${++nextId}"
        update(id) {
            it.copy(
                preview =
                    UiText.Literal(
                        input.ifBlank {
                            session.attachments
                                .first()
                                .source.name
                        },
                    ),
                // 刚刚
                updatedLabel = UiText.Resource(Res.string.just_now),
                draft = "",
                attachments = emptyList(),
                attachmentRevision = it.attachmentRevision + 1,
                runState = AgentRunState.Running,
                messages =
                    it.messages +
                        listOf(
                            ChatMessage("$messageId-user", MessageRole.User, input, attachments = it.attachments, model = it.model),
                            ChatMessage(messageId, MessageRole.Assistant, "", model = session.model),
                        ),
            )
        }
        streamReply(id, messageId, replyText)
    }

    override fun retry(id: String) {
        val session = conversations.firstOrNull { it.id == id } ?: return
        if (session.isResponding || connections.find(session.connectionId)?.isConnected != true) return
        val messageId = "message-${++nextId}"
        update(id) {
            it.copy(
                runState = AgentRunState.Running,
                messages =
                    it.messages + ChatMessage(messageId, MessageRole.Assistant, "", model = session.model),
            )
        }
        streamReply(id, messageId, replyText)
    }

    /** 切换演示状态前取消模拟回复并移除尚无内容的助手占位消息 */
    fun previewRunState(
        id: String,
        state: AgentRunState,
    ) {
        replies.remove(id)?.cancel()
        update(id) {
            it.copy(
                runState = state,
                messages =
                    it.messages.filterNot { message ->
                        message.role == MessageRole.Assistant &&
                            message.text.isEmpty()
                    },
            )
        }
    }

    /** 用协程逐段追加示例回复完成后更新状态任务可被停止或删除取消 */
    private fun streamReply(
        id: String,
        messageId: String,
        reply: String,
    ) {
        replies[id] =
            viewModelScope.launch {
                delay(350)
                for (length in 1..reply.length step 3) {
                    val text = reply.take((length + 2).coerceAtMost(reply.length))
                    update(id) { state ->
                        state.copy(messages = state.messages.map { if (it.id == messageId) it.copy(text = text) else it })
                    }
                    delay(45)
                }

                // 已完成
                update(id) { it.copy(runState = AgentRunState.Completed, preview = UiText.Resource(Res.string.reply_complete)) }
                replies.remove(id)
            }
    }

    override fun stop(id: String) {
        replies.remove(id)?.cancel()
        update(id) {
            it.copy(
                runState = AgentRunState.Stopped,
                // 已停止
                preview = UiText.Resource(Res.string.reply_stopped),
                messages = it.messages.filterNot { message -> message.role == MessageRole.Assistant && message.text.isEmpty() },
            )
        }
    }

    private fun read(id: String) = if (id == newConversation.id) newConversation else conversations.find { it.id == id }

    private fun update(
        id: String,
        transform: (ConversationUiState) -> ConversationUiState,
    ) {
        if (id == newConversation.id) {
            newConversation = transform(newConversation)
        } else {
            conversations = conversations.map { if (it.id == id) transform(it) else it }
        }
    }
}

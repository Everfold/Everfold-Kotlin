package com.everfold.kotlin.mock

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.ChatMessage
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.MessageRole
import com.everfold.kotlin.mock.longtask.OnDeviceScenarioCatalog
import com.everfold.kotlin.mock.longtask.PAUSED_SAMPLE_ASSISTANT_MESSAGE_ID
import com.everfold.kotlin.mock.longtask.PAUSED_SAMPLE_CONVERSATION_ID
import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.just_now
import everfold.shared.generated.resources.reply_stopped
import everfold.shared.generated.resources.today
import everfold.shared.generated.resources.yesterday

internal fun sampleConversations(includePausedLongTask: Boolean = false): List<ConversationUiState> =
    buildList {
        add(welcomeConversation())
        if (includePausedLongTask) add(pausedStorageAuditConversation())
        addAll(secondarySampleConversations())
    }

private fun welcomeConversation(): ConversationUiState =
    ConversationUiState(
        id = "welcome",
        title = UiText.Literal("一起整理今天的想法"),
        preview = UiText.Literal("把想法变成清晰、可以执行的下一步。"),
        // 刚刚
        updatedLabel = UiText.Resource(Res.string.just_now),
        messages =
            listOf(
                ChatMessage("welcome-1", MessageRole.User, "我想把最近的想法整理一下，你可以怎么帮我？"),
                ChatMessage(
                    "welcome-2",
                    MessageRole.Assistant,
                    "当然。我们可以先把脑海里的想法放下来，再一起找到值得推进的部分。\n\n" +
                        "01  收集想法\n不用整理顺序，直接写下你现在关注的事情。\n\n" +
                        "02  找到重点\n把相关的想法放在一起，分清目标和待解决的问题。\n\n" +
                        "03  决定下一步\n选一个今天就能开始的小行动。\n\n你最近最想推进的事情是什么？",
                ),
            ),
    )

private fun pausedStorageAuditConversation(): ConversationUiState {
    val pausedRecord = OnDeviceScenarioCatalog.preseededPausedStorageRun()
    return ConversationUiState(
        id = PAUSED_SAMPLE_CONVERSATION_ID,
        title = UiText.Literal("本机存储与下载目录整理"),
        preview = UiText.Resource(Res.string.reply_stopped),
        updatedLabel = UiText.Resource(Res.string.today),
        runState = AgentRunState.Stopped,
        messages =
            listOf(
                ChatMessage(
                    "android-storage-audit-user",
                    MessageRole.User,
                    pausedRecord.prompt,
                ),
                ChatMessage(
                    id = PAUSED_SAMPLE_ASSISTANT_MESSAGE_ID,
                    role = MessageRole.Assistant,
                    text = "",
                    execution = pausedRecord.toTrace(AgentRunState.Stopped),
                ),
            ),
    )
}

private fun secondarySampleConversations(): List<ConversationUiState> =
    listOf(
        ConversationUiState(
            id = "week",
            title = UiText.Literal("这周的工作安排"),
            preview = UiText.Literal("留出专注时间，也给变化留一点空间。"),
            // 今天
            updatedLabel = UiText.Resource(Res.string.today),
            messages =
                listOf(
                    ChatMessage("week-1", MessageRole.User, "帮我拟一份轻量的每周工作计划。"),
                    ChatMessage(
                        "week-2",
                        MessageRole.Assistant,
                        "可以先定三个本周目标。\n\n" +
                            "周一：梳理优先级与依赖。\n周二到周四：每天留一段不被打扰的专注时间。\n" +
                            "周五：回顾成果，整理下周的起点。\n\n你可以继续补充具体的工作内容。",
                    ),
                ),
        ),
        ConversationUiState(
            id = "reading",
            title = UiText.Literal("阅读笔记与灵感"),
            preview = UiText.Literal("从记录到理解，让每次阅读留下点什么。"),
            // 昨天
            updatedLabel = UiText.Resource(Res.string.yesterday),
            messages =
                listOf(
                    ChatMessage(
                        "reading-1",
                        MessageRole.Assistant,
                        "这里可以收集你的阅读摘录。\n\n每一篇笔记试着回答三个问题：\n" +
                            "- 作者想解决什么问题？\n- 哪个观点改变了我的想法？\n- 我可以把它用在哪里？\n\n" +
                            "### 记录模板\n\n可以复制下面的模板开始整理：\n\n```text\n标题：\n关键观点：\n下一步：\n```",
                    ),
                ),
        ),
    )

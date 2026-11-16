package com.everfold.kotlin.mock

import com.everfold.kotlin.feature.conversation.ConversationModel
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.SessionWorkspace
import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.model_fast
import everfold.shared.generated.resources.model_general
import everfold.shared.generated.resources.model_reasoning
import everfold.shared.generated.resources.workspace_default

/** 示例目录与模型由模拟层提供，不代表设备文件系统或真实服务能力。 */
internal object MockConversationCatalog {
    fun workspaces(connectionId: String) =
        listOf(
            SessionWorkspace("$connectionId/default", UiText.Resource(Res.string.workspace_default), "/workspace"),
            SessionWorkspace("$connectionId/everfold", UiText.Literal("Everfold"), "/workspace/everfold"),
            SessionWorkspace("$connectionId/notes", UiText.Literal("Notes"), "/workspace/notes"),
        )

    fun models(connectionId: String) =
        listOf(
            ConversationModel("$connectionId/general", UiText.Resource(Res.string.model_general)),
            ConversationModel("$connectionId/reasoning", UiText.Resource(Res.string.model_reasoning)),
            ConversationModel("$connectionId/fast", UiText.Resource(Res.string.model_fast)),
        )

    fun initialize(state: ConversationUiState) =
        state.copy(
            workspace = workspaces(state.connectionId).first(),
            model = models(state.connectionId).first(),
        )
}

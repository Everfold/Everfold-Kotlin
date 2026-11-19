package com.everfold.kotlin.feature.connections

import androidx.compose.runtime.saveable.mapSaver

/** 编辑草稿独立于已保存配置输入过程中不触发连接操作 */
internal data class ConnectionDraft(
    // null 表示沿用已保存的名称保留默认名称的资源引用
    val name: String?,
    val setup: ClusterSetup?,
    val autoConnect: Boolean,
) {
    val isValid: Boolean get() = name?.isBlank() != true && setup?.isValid != false

    companion object {
        val Saver =
            mapSaver(
                save = { draft: ConnectionDraft ->
                    // 密码与客户端私钥等敏感凭据不序列化进 SavedInstanceState，防止进程间状态转储泄露。
                    mapOf(
                        "name" to draft.name,
                        "auto" to draft.autoConnect,
                    ) + draft.setup?.toSaveableMap().orEmpty()
                },
                restore = { values ->
                    ConnectionDraft(
                        values["name"] as String?,
                        (values["method"] as String?)?.let { values.toClusterSetup() },
                        values["auto"] as Boolean,
                    )
                },
            )
    }
}

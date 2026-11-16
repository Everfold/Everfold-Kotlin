package com.everfold.kotlin.feature.connections

import androidx.compose.runtime.saveable.mapSaver

/** 编辑草稿独立于已保存配置输入过程中不触发连接操作 */
internal data class ConnectionDraft(
    val name: String,
    val setup: ClusterSetup?,
    val autoConnect: Boolean,
) {
    val isValid: Boolean get() = name.isNotBlank() && setup?.isValid != false

    companion object {
        val Saver =
            mapSaver(
                save = { draft: ConnectionDraft ->
                    mapOf(
                        "name" to draft.name,
                        "auto" to draft.autoConnect,
                        "method" to draft.setup?.method?.name,
                        "ip" to draft.setup?.ip,
                        "port" to draft.setup?.port,
                        "auth" to draft.setup?.auth?.name,
                        "account" to draft.setup?.account,
                        "password" to draft.setup?.password,
                        "server" to draft.setup?.serverCertificate,
                        "ca" to draft.setup?.caCertificate,
                        "public" to draft.setup?.clientPublicKey,
                        "private" to draft.setup?.clientPrivateKey,
                    )
                },
                restore = { values ->
                    ConnectionDraft(
                        values["name"] as String,
                        (values["method"] as String?)?.let { method ->
                            ClusterSetup(
                                method = ClusterMethod.valueOf(method),
                                ip = values["ip"] as String,
                                port = values["port"] as String,
                                auth = ClusterAuth.valueOf(values["auth"] as String),
                                account = values["account"] as String,
                                password = values["password"] as String,
                                serverCertificate = values["server"] as String?,
                                caCertificate = values["ca"] as String?,
                                clientPublicKey = values["public"] as String?,
                                clientPrivateKey = values["private"] as String?,
                            )
                        },
                        values["auto"] as Boolean,
                    )
                },
            )
    }
}

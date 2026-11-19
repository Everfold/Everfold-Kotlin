package com.everfold.kotlin.feature.connections

import androidx.compose.runtime.saveable.mapSaver

/** 集群接入方式局域网发现中心节点或云服务 */
enum class ClusterMethod { Lan, Central, Cloud }

enum class ClusterAuth { None, Password, Certificate, Mtls }

/** 连接表单数据及校验规则局域网地址可同时留空其余必填项随接入方式变化 */
data class ClusterSetup(
    val method: ClusterMethod = ClusterMethod.Lan,
    val ip: String = "",
    val port: String = "",
    val auth: ClusterAuth = ClusterAuth.None,
    val account: String = "",
    val password: String = "",
    val serverCertificate: String? = null,
    val caCertificate: String? = null,
    val clientPublicKey: String? = null,
    val clientPrivateKey: String? = null,
) {
    val invalidFields: Set<ClusterField>
        get() =
            buildSet {
                if (method != ClusterMethod.Cloud && !(method == ClusterMethod.Lan && ip.isBlank() && port.isBlank())) {
                    if (!validIp(ip.trim())) add(ClusterField.Ip)
                    if (port.trim().toIntOrNull() !in 1..65535) add(ClusterField.Port)
                }
                if (method == ClusterMethod.Cloud || (method == ClusterMethod.Central && auth == ClusterAuth.Password)) {
                    if (account.isBlank()) add(ClusterField.Account)
                    if (password.isEmpty()) add(ClusterField.Password)
                }
                if (method == ClusterMethod.Central) {
                    if (auth == ClusterAuth.Certificate && serverCertificate.isNullOrBlank()) add(ClusterField.ServerCertificate)
                    if (auth == ClusterAuth.Mtls) {
                        if (caCertificate.isNullOrBlank()) add(ClusterField.CaCertificate)
                        if (clientPublicKey.isNullOrBlank()) add(ClusterField.ClientPublicKey)
                        if (clientPrivateKey.isNullOrBlank()) add(ClusterField.ClientPrivateKey)
                    }
                }
            }
    val isValid: Boolean get() = invalidFields.isEmpty()
}

enum class ClusterField { Ip, Port, Account, Password, ServerCertificate, CaCertificate, ClientPublicKey, ClientPrivateKey }

/** 仅校验 IPv4 或 IPv6 地址格式不解析主机名或测试网络可达性 */
internal fun validIp(value: String): Boolean {
    if (':' !in value) {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { it.isNotEmpty() && it.length <= 3 && it.all(Char::isDigit) && it.toIntOrNull() in 0..255 }
    }
    return validIpv6(value)
}

/** 校验十六进制分组与一次 :: 压缩不支持 IPv4 嵌入或区域标识 */
private fun validIpv6(value: String): Boolean {
    val halves = value.split("::")
    val groups = halves.flatMap { if (it.isEmpty()) emptyList() else it.split(':') }
    val validGroups = groups.all { it.isNotEmpty() && it.length <= 4 && it.all { char -> char in "0123456789abcdefABCDEF" } }
    val validCount = if (halves.size == 2) groups.size < 8 else groups.size == 8
    return halves.size <= 2 && validGroups && validCount
}

/** 仅序列化非敏感连接配置，密码与私钥不在 SavedInstanceState 中持久化。 */
internal fun ClusterSetup.toSaveableMap(): Map<String, Any?> =
    mapOf(
        "method" to method.name,
        "ip" to ip,
        "port" to port,
        "auth" to auth.name,
        "account" to account,
        "server" to serverCertificate,
        "ca" to caCertificate,
        "public" to clientPublicKey,
    )

internal fun Map<String, Any?>.toClusterSetup(): ClusterSetup =
    ClusterSetup(
        method = ClusterMethod.valueOf(this["method"] as String),
        ip = this["ip"] as String,
        port = this["port"] as String,
        auth = ClusterAuth.valueOf(this["auth"] as String),
        account = this["account"] as String,
        password = "",
        serverCertificate = this["server"] as String?,
        caCertificate = this["ca"] as String?,
        clientPublicKey = this["public"] as String?,
        clientPrivateKey = null,
    )

internal val ClusterSetupSaver =
    mapSaver<ClusterSetup>(
        save = { it.toSaveableMap() },
        restore = { it.toClusterSetup() },
    )

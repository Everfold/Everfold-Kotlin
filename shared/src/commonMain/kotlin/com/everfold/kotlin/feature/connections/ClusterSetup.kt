package com.everfold.kotlin.feature.connections

import androidx.compose.runtime.saveable.mapSaver

private const val MIN_PORT = 1
private const val MAX_PORT = 65535
private const val IPV4_OCTET_COUNT = 4
private const val IPV4_MAX_OCTET_LENGTH = 3
private const val IPV4_MAX_OCTET = 255
private const val IPV6_MAX_GROUP_LENGTH = 4
private const val IPV6_GROUP_COUNT = 8
private const val MAX_HOSTNAME_LENGTH = 253
private const val MIN_HOSTNAME_LABELS = 2
private const val MAX_HOSTNAME_LABEL_LENGTH = 63

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
    private val isLanDiscovery: Boolean
        get() = method == ClusterMethod.Lan && ip.isBlank() && port.isBlank()

    val invalidFields: Set<ClusterField>
        get() =
            buildSet {
                if (method != ClusterMethod.Cloud && !isLanDiscovery) {
                    if (!validHost(ip.trim())) add(ClusterField.Ip)
                    if (port.trim().toIntOrNull() !in MIN_PORT..MAX_PORT) add(ClusterField.Port)
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

/** 严格校验 IPv4 或 IPv6 地址格式（拒绝前导零）不解析主机名 */
internal fun validIp(value: String): Boolean = if (':' !in value) validIpv4(value) else validIpv6(value)

private fun validIpv4(value: String): Boolean {
    val parts = value.split('.')
    return parts.size == IPV4_OCTET_COUNT && parts.all(::isValidIpv4Octet)
}

private fun isValidIpv4Octet(part: String): Boolean {
    val validSyntax =
        part.isNotEmpty() &&
            part.length <= IPV4_MAX_OCTET_LENGTH &&
            (part.length == 1 || part[0] != '0')
    return validSyntax && part.all(Char::isDigit) && part.toIntOrNull() in 0..IPV4_MAX_OCTET
}

/** 校验十六进制分组与一次 :: 压缩不支持 IPv4 嵌入或区域标识 */
private fun validIpv6(value: String): Boolean {
    val halves = value.split("::")
    val groups = halves.flatMap { if (it.isEmpty()) emptyList() else it.split(':') }
    val validGroups =
        groups.all {
            it.isNotEmpty() && it.length <= IPV6_MAX_GROUP_LENGTH && it.all { char -> char in "0123456789abcdefABCDEF" }
        }
    val validCount = if (halves.size == 2) groups.size < IPV6_GROUP_COUNT else groups.size == IPV6_GROUP_COUNT
    return halves.size <= 2 && validGroups && validCount
}

/** 支持 IPv4/IPv6、localhost 与多标签 RFC 1123 域名 */
internal fun validHost(value: String): Boolean = validIp(value) || value.equals("localhost", ignoreCase = true) || validDomainName(value)

private fun validDomainName(value: String): Boolean {
    val normalized = value.removeSuffix(".")
    val labels = normalized.split('.')
    val validLength = normalized.length in 1..MAX_HOSTNAME_LENGTH && labels.size >= MIN_HOSTNAME_LABELS
    return validLength && labels.all(::isValidHostnameLabel) && labels.last().any(Char::isAsciiLetter)
}

private fun isValidHostnameLabel(label: String): Boolean =
    label.length in 1..MAX_HOSTNAME_LABEL_LENGTH &&
        label.first().isAsciiLetterOrDigit() &&
        label.last().isAsciiLetterOrDigit() &&
        label.all { it.isAsciiLetterOrDigit() || it == '-' }

private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

private fun Char.isAsciiLetterOrDigit(): Boolean = isAsciiLetter() || this in '0'..'9'

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

package com.everfold.kotlin.feature.connections

/**
 * 页面使用的连接状态与操作契约实现负责发布可观察快照并管理异步任务。
 * 内置连接始终置顶且不可移除操作按 ID 隔离失效的 ID 不应影响其他连接。
 */
internal interface ConnectionManager {
    val connections: List<Connection>

    fun find(id: String): Connection? = connections.firstOrNull { it.id == id }

    /** 保存配置而不自动连接校验失败返回 null */
    fun add(
        name: String,
        setup: ClusterSetup,
        autoConnect: Boolean = false,
    ): String?

    /** name 为 null 时保留原名称修改接入参数时停止该连接的任务 */
    fun save(
        id: String,
        name: String?,
        setup: ClusterSetup?,
        autoConnect: Boolean,
    ): Boolean

    fun connect(id: String)

    /** 同时处理连接中取消和已连接断开保留历史信息 */
    fun disconnect(id: String)

    fun remove(id: String)
}

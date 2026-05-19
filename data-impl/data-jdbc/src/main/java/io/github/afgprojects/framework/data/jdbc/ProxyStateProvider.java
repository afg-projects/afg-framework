package io.github.afgprojects.framework.data.jdbc;

/**
 * Proxy 状态提供者接口
 * <p>
 * 允许 handler 类直接访问 proxy 的状态，避免手动同步。
 * 实现了依赖反转，让 handler 可以主动读取状态而不是被动接收。
 */
interface ProxyStateProvider {

    /**
     * 是否包含已删除记录
     *
     * @return 是否包含已删除记录
     */
    boolean isIncludeDeleted();
}

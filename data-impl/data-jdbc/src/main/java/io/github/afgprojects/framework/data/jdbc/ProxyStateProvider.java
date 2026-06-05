package io.github.afgprojects.framework.data.jdbc;

import org.jspecify.annotations.Nullable;

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

    /**
     * 获取显式设置的租户ID
     * <p>
     * 返回通过 withTenant() 显式设置的租户ID，可能为 null。
     *
     * @return 租户ID，可能为 null
     */
    @Nullable String getTenantId();

    /**
     * 解析有效的租户ID
     * <p>
     * 优先使用通过 withTenant() 显式设置的租户ID，
     * 其次回退到 TenantContextHolder 中的上下文租户ID。
     *
     * @return 有效的租户ID，可能为 null
     */
    @Nullable String resolveEffectiveTenantId();
}

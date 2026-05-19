package io.github.afgprojects.framework.data.core.tenant;

import org.jspecify.annotations.Nullable;

/**
 * 租户上下文接口
 * <p>
 * 提供租户上下文的基本操作：获取、设置、清除租户ID，以及忽略租户隔离的控制。
 * <p>
 * <b>使用场景说明：</b>
 * <ul>
 *   <li><b>ThreadLocalTenantContext</b>：简单的租户上下文实现，适用于单线程场景，
 *       仅提供基本的 ThreadLocal 存储，不涉及跨线程传播。</li>
 *   <li><b>TenantContextHolder</b>（位于 context 包）：高级租户上下文实现，
 *       支持跨线程传播、快照/恢复机制、作用域管理等，适用于异步执行、线程池等场景。</li>
 * </ul>
 * <p>
 * 推荐使用方式：
 * <ul>
 *   <li>简单场景：直接使用 {@link ThreadLocalTenantContext}</li>
 *   <li>需要跨线程传播：使用 {@link io.github.afgprojects.framework.data.core.context.TenantContextHolder}</li>
 *   <li>依赖注入：通过 Spring 注入 {@code TenantContext} 接口</li>
 * </ul>
 *
 * @see ThreadLocalTenantContext
 * @see io.github.afgprojects.framework.data.core.context.TenantContextHolder
 */
public interface TenantContext {

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，未设置时返回null
     */
    @Nullable String getTenantId();

    /**
     * 设置当前租户ID
     *
     * @param tenantId 租户ID
     */
    void setTenantId(@Nullable String tenantId);

    /**
     * 清除当前租户ID
     */
    void clear();

    /**
     * 是否忽略租户隔离
     * <p>
     * 用于系统操作或跨租户查询
     *
     * @return true表示忽略租户隔离
     */
    boolean isIgnoreTenant();

    /**
     * 设置忽略租户隔离
     *
     * @param ignore true表示忽略
     */
    void setIgnoreTenant(boolean ignore);

    /**
     * 在忽略租户隔离的上下文中执行操作
     *
     * @param runnable 要执行的操作
     */
    default void runWithoutTenant(Runnable runnable) {
        boolean oldIgnore = isIgnoreTenant();
        try {
            setIgnoreTenant(true);
            runnable.run();
        } finally {
            setIgnoreTenant(oldIgnore);
        }
    }
}

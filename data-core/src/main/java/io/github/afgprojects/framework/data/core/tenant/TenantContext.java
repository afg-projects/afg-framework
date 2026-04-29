package io.github.afgprojects.framework.data.core.tenant;

import org.jspecify.annotations.Nullable;

/**
 * 租户上下文接口
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

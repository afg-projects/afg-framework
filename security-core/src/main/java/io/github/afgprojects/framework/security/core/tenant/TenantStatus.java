package io.github.afgprojects.framework.security.core.tenant;

/**
 * 租户状态枚举。
 *
 * @since 1.0.0
 */
public enum TenantStatus {

    /**
     * 正常状态。
     */
    ACTIVE,

    /**
     * 暂停状态。
     */
    SUSPENDED,

    /**
     * 禁用状态。
     */
    DISABLED;

    /**
     * 判断是否为活跃状态。
     *
     * @return 如果是活跃状态则返回 true
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
}

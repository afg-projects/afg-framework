package io.github.afgprojects.framework.security.core.tenant;

/**
 * 租户感知接口标记。
 *
 * <p>实现此接口表示该类包含租户信息。
 *
 * <p>可用于数据隔离、审计日志等场景。
 *
 * @since 1.0.0
 */
public interface TenantAware {

    /**
     * 获取租户 ID。
     *
     * @return 租户 ID
     */
    String getTenantId();
}
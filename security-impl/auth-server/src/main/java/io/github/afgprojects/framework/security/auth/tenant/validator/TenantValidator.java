package io.github.afgprojects.framework.security.auth.tenant.validator;

import org.jspecify.annotations.NonNull;

import io.github.afgprojects.framework.security.core.tenant.TenantException;

/**
 * 租户验证器接口。
 *
 * <p>用于验证租户的有效性，包括租户是否存在、状态是否正常、是否已过期等。
 *
 * <p>实现类可以提供不同的验证策略：
 * <ul>
 *   <li>{@link DefaultTenantValidator} - 默认实现，调用 {@link io.github.afgprojects.framework.security.core.tenant.AfgTenantService} 验证</li>
 *   <li>{@link NoOpTenantValidator} - 空实现，不进行任何验证</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface TenantValidator {

    /**
     * 验证租户是否有效。
     *
     * <p>验证内容包括：
     * <ul>
     *   <li>租户是否存在</li>
     *   <li>租户状态是否为 ACTIVE</li>
     *   <li>租户是否已过期</li>
     * </ul>
     *
     * @param tenantId 租户 ID（非空）
     * @throws TenantException 如果租户验证失败
     */
    void validate(@NonNull String tenantId) throws TenantException;
}

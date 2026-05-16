package io.github.afgprojects.framework.security.auth.tenant.validator;

import org.jspecify.annotations.NonNull;

/**
 * 空操作租户验证器。
 *
 * <p>不进行任何验证，直接通过。适用于：
 * <ul>
 *   <li>开发环境，不需要租户验证</li>
 *   <li>测试环境，简化测试配置</li>
 *   <li>禁用租户验证功能的场景</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class NoOpTenantValidator implements TenantValidator {

    @Override
    public void validate(@NonNull String tenantId) {
        // 不进行任何验证
    }
}

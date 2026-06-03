package io.github.afgprojects.framework.security.auth.tenant.validator;

import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantValidator;

import org.jspecify.annotations.NonNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 空操作租户验证器。
 *
 * <p>不执行任何验证，所有租户都视为有效。
 * 当 {@link io.github.afgprojects.framework.security.core.tenant.AfgTenantService} 不可用时使用。
 *
 * @since 1.0.0
 */
@Slf4j
public class NoOpTenantValidator implements TenantValidator {

    @Override
    public void validate(@NonNull String tenantId) throws TenantException {
        log.debug("NoOpTenantValidator: 跳过租户验证: tenantId={}", tenantId);
    }
}

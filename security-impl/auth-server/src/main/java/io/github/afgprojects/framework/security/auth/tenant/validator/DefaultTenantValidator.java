package io.github.afgprojects.framework.security.auth.tenant.validator;

import com.github.benmanes.caffeine.cache.Cache;

import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantValidator;

import org.jspecify.annotations.NonNull;

import lombok.extern.slf4j.Slf4j;

/**
 * 默认租户验证器。
 *
 * <p>使用 {@link AfgTenantService} 验证租户有效性，
 * 并通过 Caffeine 缓存提升验证性能。
 *
 * @since 1.0.0
 */
@Slf4j
public class DefaultTenantValidator implements TenantValidator {

    private final AfgTenantService tenantService;
    private final Cache<String, Boolean> validationCache;

    /**
     * 构造默认租户验证器。
     *
     * @param tenantService   租户服务
     * @param validationCache 验证缓存
     */
    public DefaultTenantValidator(
            @NonNull AfgTenantService tenantService,
            @NonNull Cache<String, Boolean> validationCache) {
        this.tenantService = tenantService;
        this.validationCache = validationCache;
    }

    @Override
    public void validate(@NonNull String tenantId) throws TenantException {
        // 先查缓存
        Boolean cached = validationCache.getIfPresent(tenantId);
        if (cached != null && cached) {
            log.debug("租户验证命中缓存: tenantId={}", tenantId);
            return;
        }

        // 调用服务验证
        boolean valid = tenantService.isTenantActive(tenantId);
        if (!valid) {
            log.warn("租户验证失败: tenantId={}", tenantId);
            throw TenantException.disabled(tenantId);
        }

        // 写入缓存
        validationCache.put(tenantId, true);
        log.debug("租户验证通过: tenantId={}", tenantId);
    }
}

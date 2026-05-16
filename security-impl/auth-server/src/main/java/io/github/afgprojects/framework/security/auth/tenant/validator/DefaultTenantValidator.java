package io.github.afgprojects.framework.security.auth.tenant.validator;

import java.time.Instant;

import org.jspecify.annotations.NonNull;

import com.github.benmanes.caffeine.cache.Cache;

import io.github.afgprojects.framework.security.core.tenant.AfgTenantService;
import io.github.afgprojects.framework.security.core.tenant.Tenant;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import io.github.afgprojects.framework.security.core.tenant.TenantStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认租户验证器。
 *
 * <p>通过 {@link AfgTenantService} 获取租户信息并验证：
 * <ul>
 *   <li>租户是否存在</li>
 *   <li>租户状态是否为 ACTIVE</li>
 *   <li>租户是否已过期</li>
 * </ul>
 *
 * <p>使用 Caffeine 缓存验证结果，避免频繁调用租户服务。
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
     * @param tenantService 租户服务（非空）
     * @param validationCache 验证结果缓存（非空）
     */
    public DefaultTenantValidator(@NonNull AfgTenantService tenantService,
                                   @NonNull Cache<String, Boolean> validationCache) {
        this.tenantService = tenantService;
        this.validationCache = validationCache;
    }

    @Override
    public void validate(@NonNull String tenantId) throws TenantException {
        // 尝试从缓存获取验证结果
        Boolean cached = validationCache.getIfPresent(tenantId);
        if (cached != null) {
            log.debug("租户验证命中缓存: tenantId={}, valid={}", tenantId, cached);
            if (cached) {
                // 缓存中标记为有效，直接返回
                return;
            } else {
                // 缓存中标记为无效，抛出异常
                // 注意：缓存无效结果时，我们不知道具体原因，所以抛出通用的禁用异常
                throw TenantException.disabled(tenantId);
            }
        }

        // 执行实际验证
        doValidate(tenantId);
    }

    /**
     * 执行实际验证逻辑。
     *
     * @param tenantId 租户 ID（非空）
     * @throws TenantException 如果验证失败
     */
    private void doValidate(@NonNull String tenantId) throws TenantException {
        log.debug("验证租户: tenantId={}", tenantId);

        // 获取租户信息
        Tenant tenant = tenantService.getTenant(tenantId);

        // 检查租户是否存在
        if (tenant == null) {
            log.warn("租户不存在: tenantId={}", tenantId);
            // 缓存不存在结果（用 false 标记）
            validationCache.put(tenantId, false);
            throw TenantException.notFound(tenantId);
        }

        // 检查租户状态
        TenantStatus status = tenant.getStatus();
        if (status != TenantStatus.ACTIVE) {
            log.warn("租户状态异常: tenantId={}, status={}", tenantId, status);
            // 缓存无效结果
            validationCache.put(tenantId, false);
            throw TenantException.disabled(tenantId);
        }

        // 检查租户是否过期
        Instant expiresAt = tenant.getExpiresAt();
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            log.warn("租户已过期: tenantId={}, expiresAt={}", tenantId, expiresAt);
            // 缓存无效结果
            validationCache.put(tenantId, false);
            throw TenantException.expired(tenantId);
        }

        // 验证通过，缓存有效结果
        validationCache.put(tenantId, true);
        log.debug("租户验证通过: tenantId={}", tenantId);
    }
}

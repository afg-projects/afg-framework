package io.github.afgprojects.framework.security.core.tenant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 租户服务 SPI 接口。
 *
 * <p>定义租户查找的核心方法，业务模块需要实现此接口以提供租户数据访问能力。
 *
 * <p>实现示例：
 * <pre>{@code
 * @Service
 * public class JdbcTenantService implements AfgTenantService {
 *     private final TenantRepository tenantRepository;
 *
 *     @Override
 *     public @Nullable Tenant getTenant(@NonNull String tenantId) {
 *         return tenantRepository.findById(tenantId)
 *             .map(this::toTenant)
 *             .orElse(null);
 *     }
 *
 *     @Override
 *     public @Nullable Tenant resolveByDomain(@NonNull String domain) {
 *         return tenantRepository.findByDomain(domain)
 *             .map(this::toTenant)
 *             .orElse(null);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface AfgTenantService {

    /**
     * 根据租户 ID 获取租户。
     *
     * @param tenantId 租户 ID（非空）
     * @return 租户信息，如果不存在则返回 null
     */
    @Nullable
    Tenant getTenant(@NonNull String tenantId);

    /**
     * 检查租户是否活跃。
     *
     * <p>租户活跃需要满足以下条件：
     * <ul>
     *   <li>租户存在</li>
     *   <li>租户状态为 ACTIVE</li>
     *   <li>租户未过期（过期时间为 null 或未到达）</li>
     * </ul>
     *
     * @param tenantId 租户 ID（非空）
     * @return 如果租户活跃则返回 true，否则返回 false
     */
    default boolean isTenantActive(@NonNull String tenantId) {
        Tenant tenant = getTenant(tenantId);
        return tenant != null
            && tenant.getStatus().isActive()
            && (tenant.getExpiresAt() == null || tenant.getExpiresAt().isAfter(java.time.Instant.now()));
    }

    /**
     * 根据域名解析租户。
     *
     * <p>用于基于域名的多租户场景，如 SaaS 应用。
     * 默认返回 null，如果需要域名解析功能，需要重写此方法。
     *
     * @param domain 租户域名（非空）
     * @return 租户信息，如果不存在则返回 null
     */
    @Nullable
    default Tenant resolveByDomain(@NonNull String domain) {
        return null;
    }
}

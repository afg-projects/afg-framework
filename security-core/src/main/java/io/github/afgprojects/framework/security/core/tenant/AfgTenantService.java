package io.github.afgprojects.framework.security.core.tenant;

import java.util.Optional;

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
 *     public Optional<Tenant> findById(String tenantId) {
 *         return tenantRepository.findById(tenantId)
 *             .map(this::toTenant);
 *     }
 *
 *     @Override
 *     public Optional<Tenant> findByDomain(String domain) {
 *         return tenantRepository.findByDomain(domain)
 *             .map(this::toTenant);
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface AfgTenantService {

    /**
     * 根据租户 ID 查找租户。
     *
     * @param tenantId 租户 ID
     * @return 租户信息，如果不存在则返回 empty
     */
    Optional<Tenant> findById(String tenantId);

    /**
     * 根据域名查找租户。
     *
     * @param domain 租户域名
     * @return 租户信息，如果不存在则返回 empty
     */
    Optional<Tenant> findByDomain(String domain);
}

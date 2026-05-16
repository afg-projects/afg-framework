package io.github.afgprojects.framework.security.auth.tenant.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 租户配置属性
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   auth:
 *     tenant:
 *       enabled: true
 *       strategies:
 *         - TOKEN
 *         - HEADER
 *         - DOMAIN
 *         - DEFAULT
 *       default-tenant: default
 *       fail-if-unresolved: false
 *       header-name: X-Tenant-Id
 *       domain-mappings:
 *         tenant1.example.com: tenant-001
 *         tenant2.example.com: tenant-002
 *       validation:
 *         enabled: true
 *         cache-ttl: 5m
 * </pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.auth.tenant")
public class TenantProperties {

    /**
     * 是否启用租户功能
     */
    private boolean enabled = true;

    /**
     * 租户解析策略列表（按优先级顺序）
     */
    private List<TenantStrategy> strategies = List.of(
            TenantStrategy.TOKEN,
            TenantStrategy.HEADER,
            TenantStrategy.DOMAIN,
            TenantStrategy.DEFAULT);

    /**
     * 默认租户 ID
     */
    private String defaultTenant = "default";

    /**
     * 当无法解析租户时是否抛出异常
     */
    private boolean failIfUnresolved = false;

    /**
     * 租户请求头名称
     */
    private String headerName = "X-Tenant-Id";

    /**
     * 域名到租户 ID 的映射
     */
    private Map<String, String> domainMappings = Map.of();

    /**
     * 租户验证配置
     */
    private ValidationConfig validation = new ValidationConfig();

    /**
     * 租户解析策略
     */
    public enum TenantStrategy {
        /**
         * 从 Token 中解析租户
         */
        TOKEN,

        /**
         * 从请求头中解析租户
         */
        HEADER,

        /**
         * 从域名中解析租户
         */
        DOMAIN,

        /**
         * 使用默认租户
         */
        DEFAULT
    }

    /**
     * 租户验证配置类
     */
    @Data
    public static class ValidationConfig {

        /**
         * 是否启用租户验证
         */
        private boolean enabled = true;

        /**
         * 租户验证缓存 TTL
         */
        private Duration cacheTtl = Duration.ofMinutes(5);
    }
}

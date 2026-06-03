package io.github.afgprojects.framework.security.auth.properties.tenant;

import java.util.List;
import java.util.Map;

import io.github.afgprojects.framework.security.core.tenant.TenantResolveStrategy;

import lombok.Data;

/**
 * 租户配置。
 */
@Data
public class TenantConfig {

    /**
     * 租户解析策略列表。
     */
    private List<TenantResolveStrategy> strategies = List.of(TenantResolveStrategy.HEADER);

    /**
     * 默认租户 ID。
     */
    private String defaultTenant = "default";

    /**
     * 请求头名称（HEADER 策略使用）。
     */
    private String headerName = "X-Tenant-Id";

    /**
     * 域名映射（DOMAIN 策略使用）。
     */
    private Map<String, String> domainMappings = Map.of();

    /**
     * 无法解析租户时是否抛出异常。
     */
    private boolean failIfUnresolved = true;

    /**
     * 租户验证配置。
     */
    private ValidationConfig validation = new ValidationConfig();

    /**
     * 租户验证配置。
     */
    @Data
    public static class ValidationConfig {

        /**
         * 验证缓存 TTL。
         */
        private java.time.Duration cacheTtl = java.time.Duration.ofMinutes(5);
    }
}

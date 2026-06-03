package io.github.afgprojects.framework.security.resource.properties;

import io.github.afgprojects.framework.security.resource.properties.jwt.ResourceSecurityJwtProperties;
import io.github.afgprojects.framework.security.resource.properties.permission.ResourceSecurityPermissionProperties;
import io.github.afgprojects.framework.security.resource.properties.tenant.ResourceSecurityTenantProperties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 资源服务器统一配置属性。
 *
 * <p>整合了 JWT 验证、权限校验、租户解析等所有资源服务器相关配置。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   security:
 *     resource-server:
 *       enabled: true
 *       jwt:
 *         enabled: true
 *         jwk-set-uri: https://auth.example.com/.well-known/jwks.json
 *         issuer-uri: https://auth.example.com
 *         cache-ttl: 5m
 *       permission:
 *         auth-server-url: http://auth-server:8080/auth-api/internal
 *         key-id: resource-server-1
 *         secret: shared-secret-key
 *       tenant:
 *         strategies: token,header
 *         header-name: X-Tenant-Id
 *         fail-if-unresolved: true
 * </pre>
 *
 * @since 1.1.0
 */
@Data
@ConfigurationProperties(prefix = "afg.security.resource-server")
public class ResourceSecurityProperties {

    /**
     * 是否启用资源服务器功能。
     * 默认启用。
     */
    private boolean enabled = true;

    /**
     * 是否启用默认安全配置（放行所有请求）。
     * 默认启用。当 auth-server 同时存在时应设为 false。
     */
    private boolean defaultSecurity = true;

    /**
     * JWT 验证配置。
     */
    private ResourceSecurityJwtProperties jwt = new ResourceSecurityJwtProperties();

    /**
     * 权限校验配置。
     */
    private ResourceSecurityPermissionProperties permission = new ResourceSecurityPermissionProperties();

    /**
     * 租户解析配置。
     */
    private ResourceSecurityTenantProperties tenant = new ResourceSecurityTenantProperties();
}

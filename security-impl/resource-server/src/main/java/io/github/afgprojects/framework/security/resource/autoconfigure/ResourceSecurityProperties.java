package io.github.afgprojects.framework.security.resource.autoconfigure;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
     * JWT 验证配置。
     */
    private JwtConfig jwt = new JwtConfig();

    /**
     * 权限校验配置。
     */
    private PermissionConfig permission = new PermissionConfig();

    /**
     * 租户解析配置。
     */
    private TenantConfig tenant = new TenantConfig();

    /**
     * JWT 验证配置类。
     */
    @Data
    public static class JwtConfig {

        /**
         * 是否启用 JWT 验证。
         * 默认启用。
         */
        private boolean enabled = true;

        /**
         * JWK Set URI。
         * 用于获取公钥验证 JWT 签名。
         */
        @Nullable
        private String jwkSetUri;

        /**
         * Issuer URI。
         * 用于验证 JWT 的 iss claim。
         */
        @Nullable
        private String issuerUri;

        /**
         * JWT 公钥。
         * 可选，直接配置公钥而不通过 JWK Set URI 获取。
         */
        @Nullable
        private String publicKey;

        /**
         * JWT 签名算法。
         * 默认 RS256。
         */
        private String jwsAlgorithm = "RS256";

        /**
         * JWK Set 缓存 TTL。
         * 默认 5 分钟。
         */
        private Duration cacheTtl = Duration.ofMinutes(5);

        /**
         * 预期的 audience 集合。
         * 用于验证 JWT 的 aud claim。
         */
        private Set<String> audience = Set.of();

        /**
         * 租户 ID claim 名称。
         * 默认 tenant_id。
         */
        private String tenantIdClaim = "tenant_id";

        /**
         * 用户 ID claim 名称。
         * 默认 sub。
         */
        private String userIdClaim = "sub";

        /**
         * 用户名 claim 名称。
         * 默认 preferred_username。
         */
        private String usernameClaim = "preferred_username";

        /**
         * 角色 claim 名称。
         * 默认 roles。
         */
        private String rolesClaim = "roles";

        /**
         * 权限 claim 名称。
         * 默认 permissions。
         */
        private String permissionsClaim = "permissions";
    }

    /**
     * 权限校验配置类。
     */
    @Data
    public static class PermissionConfig {

        /**
         * 认证服务器地址。
         * 配置后将启用远程权限校验。
         */
        @Nullable
        private String authServerUrl;

        /**
         * 服务间调用密钥标识。
         * 用于签名验证，需与认证服务器配置的密钥标识一致。
         */
        @Nullable
        private String keyId;

        /**
         * 服务间调用密钥。
         * 用于生成签名，需与认证服务器配置的密钥一致。
         */
        @Nullable
        private String secret;
    }

    /**
     * 租户解析配置类。
     */
    @Data
    public static class TenantConfig {

        /**
         * 租户解析策略列表。
         * 默认按 token, header 顺序解析。
         */
        private List<String> strategies = List.of("token", "header");

        /**
         * 租户请求头名称。
         * 默认 X-Tenant-Id。
         */
        private String headerName = "X-Tenant-Id";

        /**
         * 无法解析租户时是否抛出异常。
         * 默认 true。
         */
        private boolean failIfUnresolved = true;
    }
}
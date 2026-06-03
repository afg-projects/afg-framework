package io.github.afgprojects.framework.security.resource.properties.jwt;

import java.time.Duration;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * JWT 验证配置。
 */
@Data
public class ResourceSecurityJwtProperties {

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

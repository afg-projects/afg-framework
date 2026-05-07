package io.github.afgprojects.framework.security.resource.jwt;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Set;

/**
 * JWT 资源服务器配置属性。
 *
 * <p>配置 JWT Token 验证相关参数。
 *
 * <p>配置示例：
 * <pre>
 * afg.security.resource.jwt.enabled=true
 * afg.security.resource.jwt.jwk-set-uri=https://auth.example.com/.well-known/jwks.json
 * afg.security.resource.jwt.issuer-uri=https://auth.example.com
 * afg.security.resource.jwt.cache-ttl=5m
 * </pre>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.security.resource.jwt")
public class JwtResourceProperties {

    /**
     * 是否启用 JWT 验证。
     * 默认启用。
     */
    private boolean enabled = true;

    /**
     * JWK Set URI。
     * 用于获取公钥验证 JWT 签名。
     */
    private String jwkSetUri;

    /**
     * Issuer URI。
     * 用于验证 JWT 的 iss claim。
     */
    private String issuerUri;

    /**
     * JWT 公钥。
     * 可选，直接配置公钥而不通过 JWK Set URI 获取。
     */
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(@NonNull String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    @NonNull
    public String getJwsAlgorithm() {
        return jwsAlgorithm;
    }

    public void setJwsAlgorithm(@NonNull String jwsAlgorithm) {
        this.jwsAlgorithm = jwsAlgorithm;
    }

    @NonNull
    public Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(@NonNull Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    @NonNull
    public Set<String> getAudience() {
        return audience;
    }

    public void setAudience(@NonNull Set<String> audience) {
        this.audience = audience;
    }

    @NonNull
    public String getTenantIdClaim() {
        return tenantIdClaim;
    }

    public void setTenantIdClaim(@NonNull String tenantIdClaim) {
        this.tenantIdClaim = tenantIdClaim;
    }

    @NonNull
    public String getUserIdClaim() {
        return userIdClaim;
    }

    public void setUserIdClaim(@NonNull String userIdClaim) {
        this.userIdClaim = userIdClaim;
    }

    @NonNull
    public String getUsernameClaim() {
        return usernameClaim;
    }

    public void setUsernameClaim(@NonNull String usernameClaim) {
        this.usernameClaim = usernameClaim;
    }

    @NonNull
    public String getRolesClaim() {
        return rolesClaim;
    }

    public void setRolesClaim(@NonNull String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    @NonNull
    public String getPermissionsClaim() {
        return permissionsClaim;
    }

    public void setPermissionsClaim(@NonNull String permissionsClaim) {
        this.permissionsClaim = permissionsClaim;
    }
}
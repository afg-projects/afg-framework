package io.github.afgprojects.framework.security.core.token;

import lombok.Data;

/**
 * JWT Claims 名称配置。
 *
 * <p>统一管理 JWT Token 中各 Claim 的名称映射，
 * 消除 auth-server 和 resource-server 中的硬编码。
 *
 * <p>默认值与 {@link io.github.afgprojects.framework.security.auth.token.DefaultTokenService}
 * 生成的 JWT Claims 一致。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 使用默认配置
 * JwtClaimsConfig config = new JwtClaimsConfig();
 *
 * // 自定义配置
 * JwtClaimsConfig config = new JwtClaimsConfig();
 * config.setTenantIdClaim("custom_tenant");
 * config.setRolesClaim("custom_roles");
 * }</pre>
 *
 * @since 1.0.0
 */
@Data
public class JwtClaimsConfig {

    /**
     * 默认用户 ID Claim 名称。
     */
    public static final String DEFAULT_USER_ID_CLAIM = "sub";

    /**
     * 默认用户名 Claim 名称。
     */
    public static final String DEFAULT_USERNAME_CLAIM = "preferred_username";

    /**
     * 默认角色 Claim 名称。
     */
    public static final String DEFAULT_ROLES_CLAIM = "roles";

    /**
     * 默认权限 Claim 名称。
     */
    public static final String DEFAULT_PERMISSIONS_CLAIM = "permissions";

    /**
     * 默认租户 ID Claim 名称。
     */
    public static final String DEFAULT_TENANT_ID_CLAIM = "tenant_id";

    /**
     * 默认 Token 类型 Claim 名称。
     */
    public static final String DEFAULT_TOKEN_TYPE_CLAIM = "token_type";

    /**
     * 默认 Issuer Claim 名称。
     */
    public static final String DEFAULT_ISSUER_CLAIM = "iss";

    /**
     * 用户 ID Claim 名称。
     * 默认 "sub"（JWT 标准声明）。
     */
    private String userIdClaim = DEFAULT_USER_ID_CLAIM;

    /**
     * 用户名 Claim 名称。
     * 默认 "preferred_username"（OpenID Connect 标准声明）。
     */
    private String usernameClaim = DEFAULT_USERNAME_CLAIM;

    /**
     * 角色 Claim 名称。
     * 默认 "roles"。
     */
    private String rolesClaim = DEFAULT_ROLES_CLAIM;

    /**
     * 权限 Claim 名称。
     * 默认 "permissions"。
     */
    private String permissionsClaim = DEFAULT_PERMISSIONS_CLAIM;

    /**
     * 租户 ID Claim 名称。
     * 默认 "tenant_id"。
     */
    private String tenantIdClaim = DEFAULT_TENANT_ID_CLAIM;

    /**
     * Token 类型 Claim 名称。
     * 默认 "token_type"。
     */
    private String tokenTypeClaim = DEFAULT_TOKEN_TYPE_CLAIM;

    /**
     * Issuer Claim 名称。
     * 默认 "iss"（JWT 标准声明）。
     */
    private String issuerClaim = DEFAULT_ISSUER_CLAIM;
}

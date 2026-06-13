package io.github.afgprojects.framework.security.auth.properties;

import io.github.afgprojects.framework.security.auth.properties.audit.AuditConfig;
import io.github.afgprojects.framework.security.auth.properties.casbin.CasbinConfig;
import io.github.afgprojects.framework.security.auth.properties.login.LoginConfig;
import io.github.afgprojects.framework.security.auth.properties.oauth2.OAuth2Config;
import io.github.afgprojects.framework.security.auth.properties.permission.PermissionConfig;
import io.github.afgprojects.framework.security.auth.properties.security.SecurityConfig;
import io.github.afgprojects.framework.security.auth.properties.social.SocialConfig;
import io.github.afgprojects.framework.security.auth.properties.tenant.TenantConfig;
import io.github.afgprojects.framework.security.auth.properties.token.TokenConfig;
import io.github.afgprojects.framework.security.auth.properties.totp.TotpConfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 认证服务器统一配置属性。
 *
 * <p>整合了登录、OAuth2、Token、Casbin、权限、租户、安全策略、审计等所有配置。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   security:
 *     auth-server:
 *       enabled: true
 *       token:
 *         issuer: https://auth.example.com
 *         key-store-path: file:/var/afg/keys
 *         access-token-ttl: 2h
 *         refresh-token-ttl: 7d
 *       oauth2:
 *         enabled: true
 *         authorization-code-ttl: 5m
 *         clients:
 *           - client-id: my-client
 *             client-secret: my-secret
 *             client-name: My Application
 *             redirect-uris: https://app.example.com/callback
 *             scopes: read,write
 *             grant-types: authorization_code,refresh_token
 *             require-pkce: true
 *       login:
 *         enabled: true
 *         captcha-ttl: 5m
 *         captcha-length: 4
 *       casbin:
 *         enabled: true
 *         model-type: rbac-domain
 *         policy-adapter-type: jdbc
 *         auto-save: true
 *         auto-build-role-links: true
 *       permission:
 *         enabled: true
 *         default-data-scope: ALL
 *         data-scope-interceptor-enabled: true
 *       tenant:
 *         enabled: true
 *         strategies: TOKEN,HEADER,DOMAIN,DEFAULT
 *         default-tenant: default
 *         header-name: X-Tenant-Id
 *       security:
 *         enabled: true
 *         max-login-failures: 5
 *         lock-duration: 30m
 *         max-devices: 5
 *       audit:
 *         enabled: true
 *         alert:
 *           login-failure-alert: true
 *           login-failure-threshold: 5
 * </pre>
 *
 * @since 1.1.0
 */
@Data
@ConfigurationProperties(prefix = "afg.security.auth-server")
public class AuthSecurityProperties {

    /**
     * 是否启用认证服务器功能。
     * 默认启用。
     */
    private boolean enabled = true;

    /**
     * Token 配置。
     */
    private TokenConfig token = new TokenConfig();

    /**
     * OAuth2 配置。
     */
    private OAuth2Config oauth2 = new OAuth2Config();

    /**
     * 登录配置。
     */
    private LoginConfig login = new LoginConfig();

    /**
     * Casbin 配置。
     */
    private CasbinConfig casbin = new CasbinConfig();

    /**
     * 权限配置。
     */
    private PermissionConfig permission = new PermissionConfig();

    /**
     * 租户配置。
     */
    private TenantConfig tenant = new TenantConfig();

    /**
     * 安全策略配置。
     */
    private SecurityConfig security = new SecurityConfig();

    /**
     * 社交登录配置。
     */
    private SocialConfig social = new SocialConfig();

    /**
     * TOTP 双因素认证配置。
     */
    private TotpConfig totp = new TotpConfig();

    /**
     * 审计配置。
     */
    private AuditConfig audit = new AuditConfig();
}

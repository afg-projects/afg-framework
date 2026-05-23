package io.github.afgprojects.framework.security.auth.autoconfigure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
     * 审计配置。
     */
    private AuditConfig audit = new AuditConfig();

    // ========== Token 配置 ==========

    /**
     * Token 配置类。
     */
    @Data
    public static class TokenConfig {

        /**
         * Token issuer URL。
         * 用于 JWT 的 iss claim。
         */
        private String issuer = "afg-framework";

        /**
         * RSA 密钥存储路径。
         * 默认为用户目录下的 .afg/keys。
         * 支持 file: 和 classpath: 协议。
         */
        private String keyStorePath = "file:${user.home}/.afg/keys";

        /**
         * Access Token 有效期。
         * 默认 2 小时。
         */
        private Duration accessTokenTtl = Duration.ofHours(2);

        /**
         * Refresh Token 有效期。
         * 默认 7 天。
         */
        private Duration refreshTokenTtl = Duration.ofDays(7);

        /**
         * Access Token 格式。
         * 可选值：jwt、opaque。
         * 默认 jwt。
         */
        private String accessTokenFormat = "jwt";

        /**
         * 是否在 Token 中包含用户角色。
         * 默认 true。
         */
        private boolean includeUserRoles = true;

        /**
         * 是否在 Token 中包含用户权限。
         * 默认 true。
         */
        private boolean includeUserPermissions = true;

        /**
         * 是否要求 PKCE（针对公共客户端）。
         * 默认 true。
         */
        private boolean requirePkce = true;

        /**
         * 支持的授权类型。
         */
        private Set<String> supportedGrantTypes = Set.of(
                "authorization_code",
                "client_credentials",
                "refresh_token");
    }

    // ========== OAuth2 配置 ==========

    /**
     * OAuth2 配置类。
     */
    @Data
    public static class OAuth2Config {

        /**
         * 是否启用 OAuth2 授权服务器。
         * 默认启用。
         */
        private boolean enabled = true;

        /**
         * 授权码有效期。
         * 默认 5 分钟。
         */
        private Duration authorizationCodeTtl = Duration.ofMinutes(5);

        /**
         * 预配置的客户端列表。
         */
        private Set<ClientConfig> clients = Set.of();

        /**
         * 客户端配置。
         */
        public record ClientConfig(
                @NonNull String clientId,
                String clientSecret,
                @NonNull String clientName,
                @NonNull Set<String> redirectUris,
                @NonNull Set<String> scopes,
                @NonNull Set<String> grantTypes,
                boolean requirePkce
        ) {}
    }

    // ========== 登录配置 ==========

    /**
     * 登录配置类。
     */
    @Data
    public static class LoginConfig {

        /**
         * 是否启用登录服务。
         * 默认启用。
         */
        private boolean enabled = true;

        /**
         * 验证码有效期。
         * 默认 5 分钟。
         */
        private Duration captchaTtl = Duration.ofMinutes(5);

        /**
         * 验证码长度。
         * 默认 4 位。
         */
        private int captchaLength = 4;
    }

    // ========== Casbin 配置 ==========

    /**
     * Casbin 配置类。
     */
    @Data
    public static class CasbinConfig {

        /**
         * 是否启用 Casbin 权限控制。
         */
        private boolean enabled = true;

        /**
         * 模型类型：rbac-domain, acl, rbac 等。
         */
        private String modelType = "rbac-domain";

        /**
         * 策略适配器类型：memory, jdbc, redis 等。
         */
        private String policyAdapterType = "memory";

        /**
         * 是否自动保存策略。
         */
        private boolean autoSave = true;

        /**
         * 是否自动构建角色链接。
         */
        private boolean autoBuildRoleLinks = true;

        /**
         * 自定义模型文本（可选，默认使用 rbac-domain 模型）。
         */
        @Nullable
        private String modelText;

        /**
         * Casbin 模型文件路径。
         */
        private String modelPath = "casbin/rbac_model.conf";

        /**
         * 获取默认的 RBAC with domains 模型文本。
         *
         * @return 模型文本
         */
        public String getDefaultModelText() {
            return """
                    [request_definition]
                    r = sub, dom, obj, act

                    [policy_definition]
                    p = sub, dom, obj, act

                    [role_definition]
                    g = _, _, _

                    [policy_effect]
                    e = some(where (p.eft == allow))

                    [matchers]
                    m = g(r.sub, r.dom, p.sub) && r.dom == p.dom && keyMatch2(r.obj, p.obj) && r.act == p.act
                    """;
        }
    }

    // ========== 权限配置 ==========

    /**
     * 权限配置类。
     */
    @Data
    public static class PermissionConfig {

        /**
         * 是否启用权限功能。
         */
        private boolean enabled = true;

        /**
         * 默认数据范围类型。
         * 可选值：ALL, SELF, DEPT, DEPT_AND_CHILD, CUSTOM。
         */
        private String defaultDataScope = "ALL";

        /**
         * 是否启用数据权限拦截器。
         */
        private boolean dataScopeInterceptorEnabled = true;
    }

    // ========== 租户配置 ==========

    /**
     * 租户配置类。
     */
    @Data
    public static class TenantConfig {

        /**
         * 是否启用租户功能。
         */
        private boolean enabled = true;

        /**
         * 租户解析策略列表（按优先级顺序）。
         */
        private List<TenantStrategy> strategies = List.of(
                TenantStrategy.TOKEN,
                TenantStrategy.HEADER,
                TenantStrategy.DOMAIN,
                TenantStrategy.DEFAULT);

        /**
         * 默认租户 ID。
         */
        private String defaultTenant = "default";

        /**
         * 当无法解析租户时是否抛出异常。
         */
        private boolean failIfUnresolved = false;

        /**
         * 租户请求头名称。
         */
        private String headerName = "X-Tenant-Id";

        /**
         * 域名到租户 ID 的映射。
         */
        private Map<String, String> domainMappings = Map.of();

        /**
         * 租户验证配置。
         */
        private TenantValidationConfig validation = new TenantValidationConfig();

        /**
         * 租户解析策略。
         */
        public enum TenantStrategy {
            /** 从 Token 中解析租户 */
            TOKEN,
            /** 从请求头中解析租户 */
            HEADER,
            /** 从域名中解析租户 */
            DOMAIN,
            /** 使用默认租户 */
            DEFAULT
        }

        /**
         * 租户验证配置类。
         */
        @Data
        public static class TenantValidationConfig {

            /**
             * 是否启用租户验证。
             */
            private boolean enabled = true;

            /**
             * 租户验证缓存 TTL。
             */
            private Duration cacheTtl = Duration.ofMinutes(5);
        }
    }

    // ========== 安全策略配置 ==========

    /**
     * 安全策略配置类。
     */
    @Data
    public static class SecurityConfig {

        /**
         * 是否启用安全策略。
         */
        private boolean enabled = true;

        /**
         * 最大登录失败次数。
         * 达到此次数后账户将被锁定。
         */
        private int maxLoginFailures = 5;

        /**
         * 账户锁定时长。
         */
        private Duration lockDuration = Duration.ofMinutes(30);

        /**
         * 最大设备数量。
         * 同一账号同时登录的最大设备数。
         */
        private int maxDevices = 5;

        /**
         * 密码策略配置。
         */
        private PasswordPolicyConfig passwordPolicy = new PasswordPolicyConfig();

        /**
         * IP 白名单。
         * 白名单中的 IP 可以绕过某些安全检查。
         */
        private List<String> ipWhitelist = new ArrayList<>();

        /**
         * IP 黑名单。
         * 黑名单中的 IP 将被禁止访问。
         */
        private List<String> ipBlacklist = new ArrayList<>();

        /**
         * 密码策略配置。
         */
        @Data
        public static class PasswordPolicyConfig {

            /**
             * 密码最小长度。
             */
            private int minLength = 8;

            /**
             * 是否需要大写字母。
             */
            private boolean requireUppercase = true;

            /**
             * 是否需要小写字母。
             */
            private boolean requireLowercase = true;

            /**
             * 是否需要数字。
             */
            private boolean requireDigit = true;

            /**
             * 是否需要特殊字符。
             */
            private boolean requireSpecialChar = true;

            /**
             * 转换为 PasswordPolicy 对象。
             *
             * @return PasswordPolicy 实例
             */
            public io.github.afgprojects.framework.security.auth.security.PasswordPolicy toPasswordPolicy() {
                return new io.github.afgprojects.framework.security.auth.security.PasswordPolicy(
                        minLength,
                        requireUppercase,
                        requireLowercase,
                        requireDigit,
                        requireSpecialChar);
            }
        }
    }

    // ========== 审计配置 ==========

    /**
     * 审计配置类。
     */
    @Data
    public static class AuditConfig {

        /**
         * 是否启用审计功能。
         */
        private boolean enabled = true;

        /**
         * 告警配置。
         */
        private AlertConfig alert = new AlertConfig();

        /**
         * 告警配置。
         */
        @Data
        public static class AlertConfig {

            /**
             * 是否启用登录失败告警。
             */
            private boolean loginFailureAlert = true;

            /**
             * 登录失败阈值，超过此值触发告警。
             */
            private int loginFailureThreshold = 5;

            /**
             * 是否启用新设备登录告警。
             */
            private boolean newDeviceAlert = true;

            /**
             * 是否启用新位置登录告警。
             */
            private boolean newLocationAlert = true;

            /**
             * 是否启用可疑 IP 告警。
             */
            private boolean suspiciousIpAlert = true;

            /**
             * 告警通道配置列表。
             */
            private List<AlertChannelConfig> channels = List.of(new AlertChannelConfig("log"));
        }

        /**
         * 告警通道配置。
         */
        @Data
        public static class AlertChannelConfig {

            /**
             * 通道类型。
             */
            private String type;

            /**
             * 接收者列表。
             */
            private List<String> recipients;

            public AlertChannelConfig() {
            }

            public AlertChannelConfig(String type) {
                this.type = type;
            }
        }
    }
}

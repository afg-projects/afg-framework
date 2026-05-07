package io.github.afgprojects.framework.security.auth.config;

import java.time.Duration;
import java.util.Set;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OAuth2 授权服务器配置属性
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   auth:
 *     server:
 *       enabled: true
 *       issuer: https://auth.example.com
 *       signing-key: my-secret-signing-key
 *       access-token-ttl: 2h
 *       refresh-token-ttl: 7d
 *       require-pkce: true
 *       supported-grant-types:
 *         - authorization_code
 *         - client_credentials
 *         - refresh_token
 *       token:
 *         access-token-format: jwt
 *         include-user-roles: true
 *         include-user-permissions: true
 * </pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.auth.server")
public class AuthServerProperties {

    /**
     * 是否启用授权服务器
     */
    private boolean enabled = true;

    /**
     * Token issuer URL
     */
    private String issuer;

    /**
     * JWT signing key (用于签名 JWT Token)
     */
    private String signingKey;

    /**
     * Access Token 有效期
     */
    private Duration accessTokenTtl = Duration.ofHours(2);

    /**
     * Refresh Token 有效期
     */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /**
     * 是否要求 PKCE（针对公共客户端）
     */
    private boolean requirePkce = true;

    /**
     * 支持的授权类型
     */
    private Set<String> supportedGrantTypes = Set.of(
            "authorization_code",
            "client_credentials",
            "refresh_token");

    /**
     * Token 配置
     */
    private TokenConfig token = new TokenConfig();

    /**
     * Token 配置类
     */
    @Data
    public static class TokenConfig {

        /**
         * Access Token 格式：jwt 或 opaque
         */
        private String accessTokenFormat = "jwt";

        /**
         * 是否在 Token 中包含用户角色
         */
        private boolean includeUserRoles = true;

        /**
         * 是否在 Token 中包含用户权限
         */
        private boolean includeUserPermissions = true;
    }
}
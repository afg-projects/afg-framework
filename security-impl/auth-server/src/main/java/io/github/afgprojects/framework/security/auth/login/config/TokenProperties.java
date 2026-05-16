package io.github.afgprojects.framework.security.auth.login.config;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT Token 配置属性。
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   auth:
 *     token:
 *       signing-key: my-secret-signing-key-at-least-256-bits-long
 *       issuer: https://auth.example.com
 *       access-token-ttl: 2h
 *       refresh-token-ttl: 7d
 * </pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.auth.token")
public class TokenProperties {

    /**
     * JWT 签名密钥（至少 256 位 / 32 字符）。
     *
     * <p>用于 HS256 算法签名 JWT Token。
     */
    private String signingKey;

    /**
     * Token issuer URL。
     *
     * <p>用于标识 Token 的签发者。
     */
    private String issuer = "afg-auth-server";

    /**
     * Access Token 有效期。
     *
     * <p>默认 2 小时。
     */
    private Duration accessTokenTtl = Duration.ofHours(2);

    /**
     * Refresh Token 有效期。
     *
     * <p>默认 7 天。
     */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /**
     * 是否在 Token 中包含用户角色。
     *
     * <p>默认 true。
     */
    private boolean includeRoles = true;

    /**
     * 是否在 Token 中包含用户权限。
     *
     * <p>默认 true。
     */
    private boolean includePermissions = true;
}

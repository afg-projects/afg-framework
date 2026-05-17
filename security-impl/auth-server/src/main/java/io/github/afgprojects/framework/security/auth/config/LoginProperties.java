package io.github.afgprojects.framework.security.auth.config;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录配置属性
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   auth:
 *     login:
 *       enabled: true
 *       issuer: https://auth.example.com
 *       signing-key: my-secret-signing-key-at-least-256-bits
 *       access-token-ttl: 2h
 *       refresh-token-ttl: 7d
 *       captcha-ttl: 5m
 *       captcha-length: 4
 * </pre>
 *
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "afg.auth.login")
public class LoginProperties {

    /**
     * 是否启用登录服务
     */
    private boolean enabled = true;

    /**
     * Token issuer URL
     */
    private String issuer = "afg-framework";

    /**
     * JWT signing key (用于签名 JWT Token，至少 256 位)
     */
    private String signingKey = "afg-framework-default-signing-key-must-be-at-least-256-bits";

    /**
     * Access Token 有效期
     */
    private Duration accessTokenTtl = Duration.ofHours(2);

    /**
     * Refresh Token 有效期
     */
    private Duration refreshTokenTtl = Duration.ofDays(7);

    /**
     * 验证码有效期
     */
    private Duration captchaTtl = Duration.ofMinutes(5);

    /**
     * 验证码长度
     */
    private int captchaLength = 4;
}

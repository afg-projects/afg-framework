package io.github.afgprojects.framework.security.auth.properties.token;

import java.time.Duration;
import java.util.Set;

import lombok.Data;

/**
 * Token 配置。
 */
@Data
public class TokenConfig {

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

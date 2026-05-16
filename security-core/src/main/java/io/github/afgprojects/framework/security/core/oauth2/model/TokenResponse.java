package io.github.afgprojects.framework.security.core.oauth2.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * OAuth2 令牌响应。
 *
 * @param accessToken 访问令牌
 * @param tokenType 令牌类型（Bearer）
 * @param expiresIn 过期时间（秒）
 * @param refreshToken 刷新令牌
 * @param scope 权限范围
 * @since 1.0.0
 */
public record TokenResponse(
        @NonNull String accessToken,
        @NonNull String tokenType,
        long expiresIn,
        @Nullable String refreshToken,
        @Nullable String scope) {

    /**
     * 创建令牌响应。
     *
     * @param accessToken 访问令牌
     * @param expiresIn 过期时间（秒）
     * @param refreshToken 刷新令牌
     * @param scope 权限范围
     * @return 令牌响应
     */
    public static TokenResponse of(
            @NonNull String accessToken,
            long expiresIn,
            @Nullable String refreshToken,
            @Nullable String scope) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, refreshToken, scope);
    }

    /**
     * 创建只有访问令牌的响应。
     *
     * @param accessToken 访问令牌
     * @param expiresIn 过期时间（秒）
     * @return 令牌响应
     */
    public static TokenResponse ofAccessToken(@NonNull String accessToken, long expiresIn) {
        return new TokenResponse(accessToken, "Bearer", expiresIn, null, null);
    }
}

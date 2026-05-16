package io.github.afgprojects.framework.security.auth.oauth2.controller;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 令牌自省响应。
 *
 * @param active 令牌是否有效
 * @param scope 权限范围
 * @param clientId 客户端 ID
 * @param username 用户名
 * @param tokenType 令牌类型
 * @param expiresAt 过期时间（Unix 时间戳）
 * @param issuedAt 颁发时间（Unix 时间戳）
 * @param sub 主体（用户 ID）
 * @param aud 受众
 * @param iss 签发者
 * @since 1.0.0
 */
public record IntrospectResponse(
        boolean active,
        @Nullable String scope,
        @Nullable String clientId,
        @Nullable String username,
        @Nullable String tokenType,
        @Nullable Long exp,
        @Nullable Long iat,
        @Nullable String sub,
        @Nullable String aud,
        @Nullable String iss) {

    /**
     * 创建无效令牌响应。
     *
     * @return 无效令牌响应
     */
    public static IntrospectResponse inactive() {
        return new IntrospectResponse(false, null, null, null, null, null, null, null, null, null);
    }

    /**
     * 创建有效令牌响应。
     *
     * @param scope 权限范围
     * @param clientId 客户端 ID
     * @param username 用户名
     * @param userId 用户 ID
     * @param expiresAt 过期时间
     * @param issuedAt 颁发时间
     * @return 有效令牌响应
     */
    public static IntrospectResponse active(
            @NonNull String scope,
            @NonNull String clientId,
            @Nullable String username,
            @NonNull String userId,
            long expiresAt,
            long issuedAt) {
        return new IntrospectResponse(
                true, scope, clientId, username, "Bearer",
                expiresAt, issuedAt, userId, clientId, null);
    }
}
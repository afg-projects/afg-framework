package io.github.afgprojects.framework.security.core.oauth2;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Set;

/**
 * OAuth2 访问令牌信息。
 *
 * <p>包含从访问令牌中提取的用户信息和令牌元数据。
 *
 * @param userId 用户 ID
 * @param username 用户名
 * @param clientId 客户端 ID
 * @param scopes 权限范围
 * @param tenantId 租户 ID
 * @param expiresAt 过期时间
 * @param issuedAt 颁发时间
 * @since 1.0.0
 */
public record AccessTokenInfo(
        @NonNull String userId,
        @Nullable String username,
        @NonNull String clientId,
        @NonNull Set<String> scopes,
        @Nullable String tenantId,
        @NonNull Instant expiresAt,
        @NonNull Instant issuedAt) {

    /**
     * 检查令牌是否已过期。
     *
     * @return 如果已过期返回 true
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /**
     * 检查令牌是否有效。
     *
     * @return 如果有效返回 true
     */
    public boolean isValid() {
        return !isExpired();
    }
}
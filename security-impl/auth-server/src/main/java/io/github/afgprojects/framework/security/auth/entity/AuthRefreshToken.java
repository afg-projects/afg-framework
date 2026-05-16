package io.github.afgprojects.framework.security.auth.entity;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Refresh Token 实体类。
 *
 * <p>用于持久化存储 Refresh Token 信息。
 *
 * <p>表结构定义：
 * <pre>
 * CREATE TABLE auth_refresh_token (
 *     token_id VARCHAR(64) PRIMARY KEY,
 *     token_hash VARCHAR(128) NOT NULL UNIQUE,
 *     user_id VARCHAR(64) NOT NULL,
 *     tenant_id VARCHAR(64),
 *     client_id VARCHAR(128),
 *     device_id VARCHAR(128),
 *     expires_at TIMESTAMP NOT NULL,
 *     created_at TIMESTAMP NOT NULL,
 *     INDEX idx_user_id (user_id),
 *     INDEX idx_token_hash (token_hash),
 *     INDEX idx_expires_at (expires_at)
 * );
 * </pre>
 *
 * @param tokenId    Token 唯一标识符（如 UUID）
 * @param tokenHash  Token 的哈希值（通常使用 SHA-256）
 * @param userId     用户 ID
 * @param tenantId   租户 ID（单租户场景可为 null）
 * @param clientId   客户端 ID（可选）
 * @param deviceId   设备 ID（可选）
 * @param expiresAt  过期时间
 * @param createdAt  创建时间
 * @since 1.0.0
 */
public record AuthRefreshToken(
        @NonNull String tokenId,
        @NonNull String tokenHash,
        @NonNull String userId,
        @Nullable String tenantId,
        @Nullable String clientId,
        @Nullable String deviceId,
        @NonNull LocalDateTime expiresAt,
        @NonNull LocalDateTime createdAt
) {

    /**
     * 检查 Refresh Token 是否已过期。
     *
     * @return 如果已过期返回 true，否则返回 false
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查 Refresh Token 是否有效（未过期）。
     *
     * @return 如果有效返回 true，否则返回 false
     */
    public boolean isValid() {
        return !isExpired();
    }
}

package io.github.afgprojects.framework.security.auth.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Refresh Token 实体。
 *
 * <p>用于存储 Refresh Token 信息。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "auth_refresh_token")
public class AuthRefreshToken extends BaseEntity {

    /**
     * Token 唯一标识
     */
    private String tokenId;

    /**
     * Token 哈希值（SHA-256）
     */
    private String tokenHash;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 租户 ID
     */
    private @Nullable String tenantId;

    /**
     * 客户端 ID（OAuth2）
     */
    private @Nullable String clientId;

    /**
     * 设备 ID
     */
    private @Nullable String deviceId;

    /**
     * 过期时间
     */
    private Instant expiresAt;

    /**
     * 检查 Refresh Token 是否已过期。
     *
     * @return 如果已过期返回 true，否则返回 false
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
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
package io.github.afgprojects.framework.security.auth.entity;

import lombok.Data;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Token 黑名单实体。
 *
 * <p>用于存储被撤销的 Token 信息。
 *
 * @since 1.0.0
 */
@Data
public class AuthTokenBlacklist {

    /**
     * Token 唯一标识（通常是 Token 的 SHA-256 哈希值）
     */
    private String tokenId;

    /**
     * Token 哈希值
     */
    private String tokenHash;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 加入黑名单的原因
     */
    private String reason;

    /**
     * 过期时间（Token 的原始过期时间）
     */
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
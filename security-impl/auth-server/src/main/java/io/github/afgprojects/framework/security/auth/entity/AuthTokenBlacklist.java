package io.github.afgprojects.framework.security.auth.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Token 黑名单实体。
 *
 * <p>用于存储被撤销的 Token 信息。
 *
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "auth_token_blacklist")
public class AuthTokenBlacklist extends BaseEntity {

    /**
     * Token 哈希值（SHA-256）
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
    private Instant expiresAt;
}
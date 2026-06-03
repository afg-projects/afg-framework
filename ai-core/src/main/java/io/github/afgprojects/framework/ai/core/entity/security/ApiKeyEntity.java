package io.github.afgprojects.framework.ai.core.entity.security;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI API 密钥实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_api_key")
public class ApiKeyEntity extends SoftDeleteEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "key_hash", nullable = false, length = 200)
    private String keyHash;

    @Column(name = "key_prefix", length = 20)
    private String keyPrefix;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "permissions", columnDefinition = "JSON")
    private String permissions;

    @Column(name = "allowed_origins", length = 500)
    private String allowedOrigins;

    @Column(name = "rate_limit")
    private Integer rateLimit;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "user_id", length = 64)
    private String userId;
}

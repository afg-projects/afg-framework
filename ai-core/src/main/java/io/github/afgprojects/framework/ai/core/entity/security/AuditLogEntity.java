package io.github.afgprojects.framework.ai.core.entity.security;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 审计日志实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_audit_log")
public class AuditLogEntity extends BaseEntity {

    @Column(name = "operation", nullable = false, length = 100)
    private String operation;

    @Column(name = "method", length = 100)
    private String method;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "input", columnDefinition = "TEXT")
    private String input;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "level", length = 20)
    private String level;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
}

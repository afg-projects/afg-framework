package io.github.afgprojects.framework.ai.core.entity.security;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 审计日志实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AfEntity
@Table(name = "ai_audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "create_time")
    private LocalDateTime createTime;
}

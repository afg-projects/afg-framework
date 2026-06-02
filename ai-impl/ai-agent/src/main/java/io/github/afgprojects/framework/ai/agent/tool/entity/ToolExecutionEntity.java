package io.github.afgprojects.framework.ai.agent.tool.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 工具执行记录实体
 *
 * <p>持久化存储工具执行记录，包括执行开始时间、结束时间、执行结果等信息。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "ai_tool_execution")
public class ToolExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_id", nullable = false, length = 64, unique = true)
    private String executionId;

    @Column(name = "tool_name", nullable = false, length = 200)
    private String toolName;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "arguments", columnDefinition = "TEXT")
    private String arguments;

    @Column(name = "output", columnDefinition = "TEXT")
    private String output;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

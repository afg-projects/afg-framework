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

import java.time.LocalDateTime;

/**
 * 工具注册实体
 *
 * <p>持久化存储工具注册信息，支持工具的动态注册、发现和生命周期管理。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "ai_tool_registry")
public class ToolRegistryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "input_schema", columnDefinition = "TEXT")
    private String inputSchema;

    @Column(name = "output_schema", columnDefinition = "TEXT")
    private String outputSchema;

    @Column(name = "status", length = 50)
    private String status = "ENABLED";

    @Column(name = "required_permission", length = 100)
    private String requiredPermission;

    @Column(name = "required_roles", columnDefinition = "TEXT")
    private String requiredRoles;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private Boolean deleted = false;

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

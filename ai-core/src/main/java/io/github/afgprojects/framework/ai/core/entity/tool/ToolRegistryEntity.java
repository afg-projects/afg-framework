package io.github.afgprojects.framework.ai.core.entity.tool;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 工具注册实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_tool_registry")
public class ToolRegistryEntity extends SoftDeleteEntity {

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "endpoint", length = 500)
    private String endpoint;

    @Column(name = "parameters", columnDefinition = "JSON")
    private String parameters;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "user_id", length = 64)
    private String userId;
}

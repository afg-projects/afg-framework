package io.github.afgprojects.framework.ai.core.entity.model;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 模型配置实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AfEntity
@Table(name = "ai_model_config")
public class ModelConfigEntity extends SoftDeleteEntity {

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "model_type", nullable = false, length = 50)
    private String modelType;

    @Column(name = "capabilities", columnDefinition = "JSON")
    private String capabilities;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    @Column(name = "enabled")
    private Boolean enabled = true;
}

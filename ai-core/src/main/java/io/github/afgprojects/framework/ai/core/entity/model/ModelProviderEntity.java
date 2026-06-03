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
 * AI 模型供应商实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AfEntity
@Table(name = "ai_model_provider")
public class ModelProviderEntity extends SoftDeleteEntity {

    @Column(name = "provider_name", nullable = false, length = 100)
    private String providerName;

    @Column(name = "provider_type", nullable = false, length = 50)
    private String providerType;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "api_key", length = 500)
    private String apiKey;

    @Column(name = "enabled")
    private Boolean enabled = true;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;
}

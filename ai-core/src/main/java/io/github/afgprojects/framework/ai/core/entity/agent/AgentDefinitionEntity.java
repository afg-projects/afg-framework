package io.github.afgprojects.framework.ai.core.entity.agent;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI Agent 定义实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_agent_definition")
public class AgentDefinitionEntity extends TenantEntity implements SoftDeletable {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "chat_client_name", length = 100)
    private String chatClientName;

    @Column(name = "model_name", length = 200)
    private String modelName;

    @Column(name = "temperature")
    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "max_iterations")
    private Integer maxIterations;

    @Column(name = "knowledge_base_ids", columnDefinition = "JSON")
    private String knowledgeBaseIds;

    @Column(name = "tool_ids", columnDefinition = "JSON")
    private String toolIds;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    @Column(name = "user_id", length = 64)
    private String userId;
}

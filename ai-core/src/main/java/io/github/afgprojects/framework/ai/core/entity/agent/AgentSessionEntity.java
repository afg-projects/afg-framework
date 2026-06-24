package io.github.afgprojects.framework.ai.core.entity.agent;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI Agent 会话实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_agent_session")
public class AgentSessionEntity extends SoftDeleteEntity {

    @Column(name = "agent_definition_id", nullable = false)
    private String agentDefinitionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
}

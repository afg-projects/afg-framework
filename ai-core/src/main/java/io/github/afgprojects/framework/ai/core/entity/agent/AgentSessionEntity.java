package io.github.afgprojects.framework.ai.core.entity.agent;

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
 * AI Agent 会话实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AfEntity
@Table(name = "ai_agent_session")
public class AgentSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_definition_id", nullable = false)
    private Long agentDefinitionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}

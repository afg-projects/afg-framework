package io.github.afgprojects.framework.ai.core.entity.workflow;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 工作流执行记录实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_workflow_execution")
public class WorkflowExecutionEntity extends BaseEntity {

    @Column(name = "workflow_definition_id", nullable = false)
    private String workflowDefinitionId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "input", columnDefinition = "JSON")
    private String input;

    @Column(name = "output", columnDefinition = "JSON")
    private String output;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "user_id", length = 64)
    private String userId;
}

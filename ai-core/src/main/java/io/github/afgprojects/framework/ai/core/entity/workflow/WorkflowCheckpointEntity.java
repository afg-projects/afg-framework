package io.github.afgprojects.framework.ai.core.entity.workflow;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 工作流检查点实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_workflow_checkpoint")
public class WorkflowCheckpointEntity extends BaseEntity {

    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "workflow_id", nullable = false, length = 100)
    private String workflowId;

    @Column(name = "current_node_id", length = 100)
    private String currentNodeId;

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;
}

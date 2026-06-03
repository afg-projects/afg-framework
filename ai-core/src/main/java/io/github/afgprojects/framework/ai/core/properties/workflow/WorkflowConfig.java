package io.github.afgprojects.framework.ai.core.properties.workflow;

import lombok.Data;

/**
 * 工作流配置。
 */
@Data
public class WorkflowConfig {

    /**
     * 是否启用工作流。
     */
    private boolean enabled = true;

    /**
     * 最大并行节点数。
     */
    private int maxParallelNodes = 10;

    /**
     * 检查点策略。
     */
    private CheckpointPolicy checkpointPolicy = CheckpointPolicy.EVERY_STAGE;
}

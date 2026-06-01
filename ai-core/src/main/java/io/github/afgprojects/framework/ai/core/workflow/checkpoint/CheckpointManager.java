package io.github.afgprojects.framework.ai.core.workflow.checkpoint;

import io.github.afgprojects.framework.ai.core.workflow.engine.ExecutionContext;

public interface CheckpointManager {
    void save(String executionId, ExecutionContext context, String currentNodeId);
    Checkpoint load(String executionId);
    void complete(String executionId);

    record Checkpoint(
        String executionId,
        String workflowId,
        String currentNodeId,
        long createdAt
    ) {}
}

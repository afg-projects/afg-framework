package io.github.afgprojects.framework.ai.core.workflow;

import java.util.concurrent.ConcurrentHashMap;

import io.github.afgprojects.framework.ai.core.api.workflow.checkpoint.CheckpointManager;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InMemoryCheckpointManager implements CheckpointManager {

    private final ConcurrentHashMap<String, Checkpoint> checkpoints = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ExecutionContext> contexts = new ConcurrentHashMap<>();

    @Override
    public void save(String executionId, ExecutionContext context, String currentNodeId) {
        Checkpoint checkpoint = new Checkpoint(
            executionId,
            context.getWorkflowId(),
            currentNodeId,
            System.currentTimeMillis()
        );
        checkpoints.put(executionId, checkpoint);
        contexts.put(executionId, context);
        log.debug("Saved checkpoint for executionId={}, currentNodeId={}", executionId, currentNodeId);
    }

    @Override
    public Checkpoint load(String executionId) {
        Checkpoint checkpoint = checkpoints.get(executionId);
        if (checkpoint == null) {
            log.debug("No checkpoint found for executionId={}", executionId);
        }
        return checkpoint;
    }

    @Override
    public void complete(String executionId) {
        Checkpoint removed = checkpoints.remove(executionId);
        contexts.remove(executionId);
        if (removed != null) {
            log.debug("Completed and removed checkpoint for executionId={}", executionId);
        } else {
            log.debug("No checkpoint to complete for executionId={}", executionId);
        }
    }

    public ExecutionContext loadContext(String executionId) {
        return contexts.get(executionId);
    }
}

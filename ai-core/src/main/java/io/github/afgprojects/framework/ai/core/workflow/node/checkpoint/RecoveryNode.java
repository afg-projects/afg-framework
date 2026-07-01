package io.github.afgprojects.framework.ai.core.workflow.node.checkpoint;

import io.github.afgprojects.framework.ai.core.api.workflow.checkpoint.CheckpointManager;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recovery node - restores workflow execution from a checkpoint.
 *
 * <p>Loads a previously saved checkpoint and restores the workflow execution
 * state. Used to resume workflows that were interrupted or failed.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link CheckpointManager} is a construction-time dependency; the no-arg
 * constructor leaves it null so the node degrades gracefully when checkpointing
 * is not configured.</p>
 */
@Slf4j
public class RecoveryNode extends AbstractWorkflowNode<RecoveryNode.Params> {

    public static final String TYPE = "recovery";

    /** Strongly-typed parameters for {@link RecoveryNode}. */
    public record Params(
            @Param(displayName = "Execution ID", description = "The execution ID to recover from", required = true)
            String executionId,
            @Param(displayName = "Fail if not found", description = "Whether to fail if no checkpoint exists", defaultValue = "false")
            Boolean failIfNotFound
    ) {
        /** Whether to fail if the checkpoint is not found. */
        public boolean isFailIfNotFound() {
            return Boolean.TRUE.equals(failIfNotFound);
        }
    }

    /** Output descriptor for {@link RecoveryNode}. */
    public record Output(
            @Out(description = "Whether recovered") boolean recovered,
            @Out(description = "Execution ID") String executionId
    ) {}

    private final CheckpointManager checkpointManager;

    public RecoveryNode(String nodeId, CheckpointManager checkpointManager) {
        super(nodeId, TYPE, Params.class);
        this.checkpointManager = checkpointManager;
    }

    public RecoveryNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.checkpointManager = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String executionId = params.executionId();
        boolean failIfNotFound = params.isFailIfNotFound();

        log.debug("RecoveryNode [{}] recovering from checkpoint: {}", getNodeId(), executionId);

        if (checkpointManager == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("recovered", false);
            result.put("executionId", executionId);
            result.put("message", "No CheckpointManager available - recovery not possible");
            return result;
        }

        CheckpointManager.Checkpoint checkpoint = checkpointManager.load(executionId);

        Map<String, Object> result = new LinkedHashMap<>();
        if (checkpoint != null) {
            result.put("recovered", true);
            result.put("executionId", checkpoint.executionId());
            result.put("workflowId", checkpoint.workflowId());
            result.put("currentNodeId", checkpoint.currentNodeId());
        } else {
            result.put("recovered", false);
            result.put("executionId", executionId);
            if (failIfNotFound) {
                throw new IllegalStateException("Checkpoint not found for execution: " + executionId);
            }
        }

        return result;
    }
}

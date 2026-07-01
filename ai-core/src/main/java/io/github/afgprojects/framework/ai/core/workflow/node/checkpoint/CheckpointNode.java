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
 * Checkpoint node - saves the current workflow execution state.
 *
 * <p>Creates a checkpoint of the current workflow execution state, allowing
 * the workflow to be resumed from this point if it fails or is interrupted.
 * Essential for long-running workflows that need fault tolerance.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * The {@link CheckpointManager} is a construction-time dependency; the no-arg
 * constructor leaves it null so the node degrades gracefully when checkpointing
 * is not configured.</p>
 */
@Slf4j
public class CheckpointNode extends AbstractWorkflowNode<CheckpointNode.Params> {

    public static final String TYPE = "checkpoint";

    /** Strongly-typed parameters for {@link CheckpointNode}. */
    public record Params(
            @Param(displayName = "Label", description = "Descriptive label for this checkpoint")
            String label,
            @Param(displayName = "Metadata", description = "Additional metadata to store with the checkpoint")
            Map<String, Object> metadata
    ) {
        /** Effective label, defaulting to "checkpoint-&lt;nodeId&gt;". */
        public String effectiveLabel(String nodeId) {
            return label == null || label.isBlank() ? "checkpoint-" + nodeId : label;
        }
    }

    /** Output descriptor for {@link CheckpointNode}. */
    public record Output(
            @Out(description = "Whether checkpoint saved") boolean checkpointSaved,
            @Out(description = "Label") String label,
            @Out(description = "Workflow ID") String workflowId
    ) {}

    private final CheckpointManager checkpointManager;

    public CheckpointNode(String nodeId, CheckpointManager checkpointManager) {
        super(nodeId, TYPE, Params.class);
        this.checkpointManager = checkpointManager;
    }

    public CheckpointNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.checkpointManager = null;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        String label = params.effectiveLabel(getNodeId());

        log.debug("CheckpointNode [{}] saving checkpoint: {}", getNodeId(), label);

        if (checkpointManager == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("checkpointSaved", false);
            result.put("label", label);
            result.put("message", "No CheckpointManager available - checkpoint not saved");
            return result;
        }

        checkpointManager.save(context.getWorkflowId(), context, getNodeId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkpointSaved", true);
        result.put("label", label);
        result.put("workflowId", context.getWorkflowId());
        result.put("currentNodeId", getNodeId());
        return result;
    }
}

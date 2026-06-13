package io.github.afgprojects.framework.ai.core.workflow.node.checkpoint;

import io.github.afgprojects.framework.ai.core.api.workflow.checkpoint.CheckpointManager;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checkpoint node - saves the current workflow execution state.
 *
 * <p>Creates a checkpoint of the current workflow execution state, allowing
 * the workflow to be resumed from this point if it fails or is interrupted.
 * Essential for long-running workflows that need fault tolerance.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code label} (optional) - descriptive label for this checkpoint</li>
 *   <li>{@code metadata} (optional) - additional metadata to store with the checkpoint</li>
 * </ul>
 */
@Slf4j
public class CheckpointNode implements WorkflowNode {

    public static final String TYPE = "checkpoint";

    private final String nodeId;
    private final CheckpointManager checkpointManager;

    public CheckpointNode(String nodeId, CheckpointManager checkpointManager) {
        this.nodeId = nodeId;
        this.checkpointManager = checkpointManager;
    }

    public CheckpointNode(String nodeId) {
        this.nodeId = nodeId;
        this.checkpointManager = null;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            String label = getParam(params, "label", "checkpoint-" + nodeId);

            log.debug("CheckpointNode [{}] saving checkpoint: {}", nodeId, label);

            if (checkpointManager == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("checkpointSaved", false);
                result.put("label", label);
                result.put("message", "No CheckpointManager available - checkpoint not saved");
                long duration = System.currentTimeMillis() - startTime;
                return NodeOutput.of(result).withDuration(duration);
            }

            checkpointManager.save(context.getWorkflowId(), context, nodeId);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("checkpointSaved", true);
            result.put("label", label);
            result.put("workflowId", context.getWorkflowId());
            result.put("currentNodeId", nodeId);

            long duration = System.currentTimeMillis() - startTime;
            return NodeOutput.of(result).withDuration(duration);

        } catch (Exception e) {
            log.error("CheckpointNode [{}] execution failed", nodeId, e);
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            return NodeOutput.of(errorData).withDuration(duration);
        }
    }

    @Override
    public Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
        return Flux.just(NodeEvent.complete(execute(context, params)));
    }

    private String getParam(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}

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
 * Recovery node - restores workflow execution from a checkpoint.
 *
 * <p>Loads a previously saved checkpoint and restores the workflow execution
 * state. Used to resume workflows that were interrupted or failed.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code executionId} (required) - the execution ID to recover from</li>
 *   <li>{@code failIfNotFound} (optional) - whether to fail if no checkpoint exists, defaults to false</li>
 * </ul>
 */
@Slf4j
public class RecoveryNode implements WorkflowNode {

    public static final String TYPE = "recovery";

    private final String nodeId;
    private final CheckpointManager checkpointManager;

    public RecoveryNode(String nodeId, CheckpointManager checkpointManager) {
        this.nodeId = nodeId;
        this.checkpointManager = checkpointManager;
    }

    public RecoveryNode(String nodeId) {
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
            String executionId = getRequiredParam(params, "executionId");
            boolean failIfNotFound = getBooleanParam(params, "failIfNotFound", false);

            log.debug("RecoveryNode [{}] recovering from checkpoint: {}", nodeId, executionId);

            if (checkpointManager == null) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("recovered", false);
                result.put("executionId", executionId);
                result.put("message", "No CheckpointManager available - recovery not possible");
                long duration = System.currentTimeMillis() - startTime;
                return NodeOutput.of(result).withDuration(duration);
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

            long duration = System.currentTimeMillis() - startTime;
            return NodeOutput.of(result).withDuration(duration);

        } catch (Exception e) {
            log.error("RecoveryNode [{}] execution failed", nodeId, e);
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

    private String getRequiredParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Required parameter '" + key + "' is missing");
        }
        return value.toString();
    }

    private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean bool) return bool;
        return Boolean.parseBoolean(value.toString());
    }
}

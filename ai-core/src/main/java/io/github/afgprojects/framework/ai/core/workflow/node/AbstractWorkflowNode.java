package io.github.afgprojects.framework.ai.core.workflow.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for workflow nodes providing common execution lifecycle:
 * timing, error handling, logging, and default streaming behavior.
 *
 * <p>Subclasses implement {@link #doExecute} for the actual node logic.
 * The base class wraps execution with start/end timing and catches exceptions
 * to produce a standardized error {@link NodeOutput}.</p>
 *
 * <p>Default streaming delegates to the synchronous {@link #execute} method.
 * Nodes that need true streaming should override {@link #executeStream}.</p>
 */
@Slf4j
public abstract class AbstractWorkflowNode implements WorkflowNode {

    private final String nodeId;
    private final String type;

    protected AbstractWorkflowNode(String nodeId, String type) {
        this.nodeId = nodeId;
        this.type = type;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public NodeOutput execute(ExecutionContext context, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        try {
            log.debug("Node [{}] (type={}) executing", nodeId, type);
            Map<String, Object> result = doExecute(context, params != null ? params : Map.of());
            long duration = System.currentTimeMillis() - startTime;
            return NodeOutput.of(result).withDuration(duration);
        } catch (Exception e) {
            log.error("Node [{}] (type={}) execution failed", nodeId, type, e);
            long duration = System.currentTimeMillis() - startTime;
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("error", e.getMessage() != null ? e.getMessage() : "Unknown error");
            errorData.put("nodeType", type);
            return NodeOutput.of(errorData).withDuration(duration);
        }
    }

    @Override
    public Flux<NodeEvent> executeStream(ExecutionContext context, Map<String, Object> params) {
        NodeOutput output = execute(context, params);
        return Flux.just(NodeEvent.complete(output));
    }

    /**
     * Execute the node-specific logic. Subclasses must implement this method.
     *
     * @param context the workflow execution context
     * @param params  the node parameters (never null)
     * @return a map of output data
     */
    protected abstract Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params);
}

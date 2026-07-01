package io.github.afgprojects.framework.ai.core.workflow.node;

/**
 * Thrown when a workflow node's runtime parameters cannot be bound to its
 * strongly-typed params record — missing required value, type mismatch, or
 * illegal enum value.
 *
 * <p>Carries the nodeId and offending parameter name so callers can produce an
 * error that locates the failure precisely within the workflow DAG.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ParamBindingException extends RuntimeException {

    private final String nodeId;
    private final String paramName;

    public ParamBindingException(String nodeId, String paramName, String reason) {
        super("Node [" + nodeId + "] parameter '" + paramName + "': " + reason);
        this.nodeId = nodeId;
        this.paramName = paramName;
    }

    public ParamBindingException(String nodeId, String paramName, String reason, Throwable cause) {
        super("Node [" + nodeId + "] parameter '" + paramName + "': " + reason, cause);
        this.nodeId = nodeId;
        this.paramName = paramName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getParamName() {
        return paramName;
    }
}

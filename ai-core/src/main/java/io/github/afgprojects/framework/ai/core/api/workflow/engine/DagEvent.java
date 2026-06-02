package io.github.afgprojects.framework.ai.core.api.workflow.engine;

public record DagEvent(
    String type,
    String nodeId,
    Object data
) {
    public static DagEvent nodeStart(String nodeId) {
        return new DagEvent("NODE_START", nodeId, null);
    }

    public static DagEvent nodeComplete(String nodeId, NodeOutput output) {
        return new DagEvent("NODE_COMPLETE", nodeId, output);
    }

    public static DagEvent nodeError(String nodeId, String error) {
        return new DagEvent("NODE_ERROR", nodeId, error);
    }

    public static DagEvent workflowComplete(DagResult result) {
        return new DagEvent("WORKFLOW_COMPLETE", null, result);
    }
}

package io.github.afgprojects.framework.ai.core.workflow.engine;

import java.util.Map;

public interface ExecutionContext {
    String getWorkflowId();
    String getConversationId();
    String getUserId();
    Map<String, Object> getVariables();
    Map<String, NodeOutput> getNodeOutputs();
    Object resolveVariable(String expression);
    void setNodeOutput(String nodeId, NodeOutput output);
}

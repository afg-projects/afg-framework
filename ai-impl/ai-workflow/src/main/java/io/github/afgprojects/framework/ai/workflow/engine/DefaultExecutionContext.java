package io.github.afgprojects.framework.ai.workflow.engine;

import io.github.afgprojects.framework.ai.core.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.engine.NodeOutput;
import io.github.afgprojects.framework.ai.workflow.dsl.DefaultVariableResolver;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of ExecutionContext.
 */
public class DefaultExecutionContext implements ExecutionContext {

    private final String workflowId;
    private final String conversationId;
    private final String userId;
    private final Map<String, Object> variables;
    private final Map<String, NodeOutput> nodeOutputs;
    private final DefaultVariableResolver variableResolver;

    public DefaultExecutionContext(String workflowId, String conversationId, String userId) {
        this(workflowId, conversationId, userId, Collections.emptyMap());
    }

    public DefaultExecutionContext(String workflowId, String conversationId, String userId,
                                   Map<String, Object> variables) {
        this.workflowId = workflowId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.variables = Map.copyOf(variables);
        this.nodeOutputs = new ConcurrentHashMap<>();
        this.variableResolver = new DefaultVariableResolver();
    }

    @Override
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    public String getConversationId() {
        return conversationId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public Map<String, Object> getVariables() {
        return variables;
    }

    @Override
    public Map<String, NodeOutput> getNodeOutputs() {
        return Collections.unmodifiableMap(nodeOutputs);
    }

    @Override
    public Object resolveVariable(String expression) {
        return variableResolver.resolve(expression, nodeOutputs);
    }

    @Override
    public void setNodeOutput(String nodeId, NodeOutput output) {
        nodeOutputs.put(nodeId, output);
    }
}

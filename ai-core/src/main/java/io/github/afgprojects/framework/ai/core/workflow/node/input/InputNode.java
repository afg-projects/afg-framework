package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Input node - provides static input data to the workflow.
 *
 * <p>Passes through the configured parameters as the node output.
 * Typically used as the first node in a workflow to provide initial data.</p>
 */
@Slf4j
public class InputNode extends AbstractWorkflowNode {

    public static final String TYPE = "input";

    public InputNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        log.debug("InputNode [{}] passing through {} params", getNodeId(), params.size());
        // Pass through all params as output data
        Map<String, Object> result = new LinkedHashMap<>(params);
        // Also include workflow variables if present
        if (context.getVariables() != null && !context.getVariables().isEmpty()) {
            result.putIfAbsent("variables", context.getVariables());
        }
        return result;
    }
}

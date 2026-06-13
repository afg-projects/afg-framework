package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Output node - produces the final output of a workflow branch.
 *
 * <p>Collects data from the workflow context and produces the final
 * output. Typically used as a terminal node to format and present
 * results.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code outputKeys} (optional) - list of keys to include in output (null = all)</li>
 *   <li>{@code format} (optional) - output format, defaults to "raw"</li>
 * </ul>
 */
@Slf4j
public class OutputNode extends AbstractWorkflowNode {

    public static final String TYPE = "output";

    public OutputNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        log.debug("OutputNode [{}] collecting output", getNodeId());

        // Collect all params as output, optionally filtering by outputKeys
        Map<String, Object> result = new LinkedHashMap<>(params);

        // Remove internal parameters
        result.remove("outputKeys");
        result.remove("format");

        return result;
    }
}

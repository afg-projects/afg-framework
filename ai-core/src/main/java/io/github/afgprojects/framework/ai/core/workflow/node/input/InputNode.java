package io.github.afgprojects.framework.ai.core.workflow.node.input;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Input node - provides static input data to the workflow.
 *
 * <p>Passes the configured {@code data} parameter through as the node output.
 * Typically used as the first node in a workflow to provide initial data;
 * workflow variables are also surfaced under the {@code variables} key when
 * present.</p>
 */
@Slf4j
public class InputNode extends AbstractWorkflowNode<InputNode.Params> {

    public static final String TYPE = "input";

    /** Strongly-typed parameters for {@link InputNode}. */
    public record Params(
            @Param(displayName = "Input data", description = "Input data")
            Map<String, Object> data
    ) {}

    /** Output descriptor for {@link InputNode}. */
    public record Output(
            @Out(description = "Input data") Map<String, Object> data
    ) {}

    public InputNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        log.debug("InputNode [{}] passing through data", getNodeId());
        // Pass through the configured data as output
        Map<String, Object> result = new LinkedHashMap<>();
        if (params.data() != null) {
            result.putAll(params.data());
        }
        // Also include workflow variables if present
        if (context.getVariables() != null && !context.getVariables().isEmpty()) {
            result.putIfAbsent("variables", context.getVariables());
        }
        return result;
    }
}

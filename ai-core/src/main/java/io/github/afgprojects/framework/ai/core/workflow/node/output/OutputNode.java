package io.github.afgprojects.framework.ai.core.workflow.node.output;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
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
 * <p>Parameters are declared on {@link Params} so the node is self-describing.</p>
 */
@Slf4j
public class OutputNode extends AbstractWorkflowNode<OutputNode.Params> {

    public static final String TYPE = "output";

    /** Strongly-typed parameters for {@link OutputNode}. */
    public record Params(
            @Param(displayName = "Format", description = "Output format", defaultValue = "raw")
            String format,
            @Param(displayName = "Data", description = "Output data")
            Object data
    ) {
        /** Effective format, defaulting to "raw". */
        public String effectiveFormat() {
            return format == null || format.isBlank() ? "raw" : format;
        }
    }

    /** Output descriptor for {@link OutputNode}. */
    public record Output(
            @Out(description = "Output data") Object data
    ) {}

    public OutputNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        log.debug("OutputNode [{}] collecting output", getNodeId());

        Map<String, Object> result = new LinkedHashMap<>();
        Object data = params.data();
        if (data instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) map;
            result.putAll(dataMap);
        } else if (data != null) {
            result.put("data", data);
        }
        result.put("format", params.effectiveFormat());
        return result;
    }
}

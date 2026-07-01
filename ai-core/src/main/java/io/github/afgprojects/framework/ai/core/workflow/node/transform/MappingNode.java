package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapping node - maps fields from input to output with optional renaming.
 *
 * <p>Transforms input data by mapping fields from source names to target names.
 * Supports field renaming and default values for missing fields.</p>
 *
 * <p>Parameters are declared on {@link Params}.</p>
 */
@Slf4j
public class MappingNode extends AbstractWorkflowNode<MappingNode.Params> {

    public static final String TYPE = "mapping";

    /** Strongly-typed parameters for {@link MappingNode}. */
    public record Params(
            @Param(displayName = "Input data", description = "Input data map", required = true)
            Map<String, Object> input,
            @Param(displayName = "Field mapping", description = "Map of sourceField -> targetField for field mapping", required = true)
            Map<String, String> mapping,
            @Param(displayName = "Default values", description = "Map of field -> defaultValue for missing fields")
            Map<String, Object> defaults
    ) {}

    /** Output descriptor for {@link MappingNode}. */
    public record Output(
            @Out(description = "Mapped data") Map<String, Object> data
    ) {}

    public MappingNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        Map<String, String> mapping = params.mapping();
        if (mapping == null || mapping.isEmpty()) {
            throw new IllegalArgumentException("Required parameter 'mapping' must be a non-empty map");
        }

        Map<String, Object> sourceData = params.input();
        if (sourceData == null) {
            throw new IllegalArgumentException("Required parameter 'input' is missing");
        }

        log.debug("MappingNode [{}] mapping {} fields", getNodeId(), mapping.size());

        Map<String, Object> result = new LinkedHashMap<>();

        // Apply field mapping
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String sourceField = entry.getKey();
            String targetField = entry.getValue();
            Object value = sourceData.get(sourceField);
            if (value != null) {
                result.put(targetField, value);
            }
        }

        // Apply defaults for missing fields
        Map<String, Object> defaults = params.defaults();
        if (defaults != null) {
            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }
}

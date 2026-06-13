package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mapping node - maps fields from input to output with optional renaming.
 *
 * <p>Transforms input data by mapping fields from source names to target names.
 * Supports field renaming, type conversion hints, and default values.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code input} (required) - input data map</li>
 *   <li>{@code mapping} (required) - Map of sourceField -> targetField for field mapping</li>
 *   <li>{@code defaults} (optional) - Map of field -> defaultValue for missing fields</li>
 * </ul>
 */
@Slf4j
public class MappingNode extends AbstractWorkflowNode {

    public static final String TYPE = "mapping";

    public MappingNode(String nodeId) {
        super(nodeId, TYPE);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        Object input = params.get("input");
        if (input == null) {
            throw new IllegalArgumentException("Required parameter 'input' is missing");
        }

        Map<String, String> mapping = (Map<String, String>) params.get("mapping");
        if (mapping == null || mapping.isEmpty()) {
            throw new IllegalArgumentException("Required parameter 'mapping' must be a non-empty map");
        }

        Map<String, Object> defaults = (Map<String, Object>) params.get("defaults");

        log.debug("MappingNode [{}] mapping {} fields", getNodeId(), mapping.size());

        Map<String, Object> sourceData;
        if (input instanceof Map<?, ?> map) {
            sourceData = (Map<String, Object>) map;
        } else {
            throw new IllegalArgumentException("Input must be a Map");
        }

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
        if (defaults != null) {
            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                result.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }
}

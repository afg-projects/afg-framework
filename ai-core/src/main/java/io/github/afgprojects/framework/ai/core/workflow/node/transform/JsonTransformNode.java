package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON transform node - transforms JSON data using path expressions.
 *
 * <p>Applies JSON path-based transformations to input data. Supports
 * extracting, renaming, and restructuring JSON fields.</p>
 *
 * <p>Parameters:</p>
 * <ul>
 *   <li>{@code input} (required) - input JSON data (String or Map)</li>
 *   <li>{@code operations} (optional) - list of transform operations</li>
 *   <li>{@code extractPaths} (optional) - list of JSON paths to extract</li>
 *   <li>{@code addFields} (optional) - Map of field names to values to add</li>
 *   <li>{@code removeFields} (optional) - list of field names to remove</li>
 * </ul>
 */
@Slf4j
public class JsonTransformNode extends AbstractWorkflowNode {

    public static final String TYPE = "json-transform";

    private final ObjectMapper objectMapper;

    public JsonTransformNode(String nodeId) {
        super(nodeId, TYPE);
        this.objectMapper = new ObjectMapper();
    }

    public JsonTransformNode(String nodeId, ObjectMapper objectMapper) {
        super(nodeId, TYPE);
        this.objectMapper = objectMapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(ExecutionContext context, Map<String, Object> params) {
        Object input = params.get("input");
        if (input == null) {
            throw new IllegalArgumentException("Required parameter 'input' is missing");
        }

        log.debug("JsonTransformNode [{}] transforming data", getNodeId());

        // Convert input to a mutable map
        Map<String, Object> data;
        if (input instanceof Map<?, ?> map) {
            data = new LinkedHashMap<>((Map<String, Object>) map);
        } else if (input instanceof String jsonStr) {
            try {
                data = objectMapper.readValue(jsonStr, LinkedHashMap.class);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Invalid JSON input: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("Input must be a Map or JSON string");
        }

        // Remove fields
        Object removeFields = params.get("removeFields");
        if (removeFields instanceof Iterable<?> fields) {
            for (Object field : fields) {
                data.remove(field.toString());
            }
        }

        // Add fields
        @SuppressWarnings("unchecked")
        Map<String, Object> addFields = (Map<String, Object>) params.get("addFields");
        if (addFields != null) {
            data.putAll(addFields);
        }

        return data;
    }
}

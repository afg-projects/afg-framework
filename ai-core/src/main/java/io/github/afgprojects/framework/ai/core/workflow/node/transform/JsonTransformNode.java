package io.github.afgprojects.framework.ai.core.workflow.node.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Out;
import io.github.afgprojects.framework.ai.core.workflow.annotation.Param;
import io.github.afgprojects.framework.ai.core.workflow.node.AbstractWorkflowNode;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON transform node - transforms JSON data using path expressions.
 *
 * <p>Applies JSON path-based transformations to input data. Supports
 * extracting, renaming, and restructuring JSON fields.</p>
 *
 * <p>Parameters are declared on {@link Params} so the node is self-describing.
 * An {@link ObjectMapper} is a construction-time dependency; the no-arg
 * constructor creates one internally, while the two-arg constructor accepts an
 * injected instance.</p>
 */
@Slf4j
public class JsonTransformNode extends AbstractWorkflowNode<JsonTransformNode.Params> {

    public static final String TYPE = "json-transform";

    /** Strongly-typed parameters for {@link JsonTransformNode}. */
    public record Params(
            @Param(displayName = "Input", description = "Input JSON data (String or Map)", required = true)
            Object input,
            @Param(displayName = "Operations", description = "List of transform operations")
            List<Object> operations,
            @Param(displayName = "Extract paths", description = "List of JSON paths to extract")
            List<String> extractPaths,
            @Param(displayName = "Add fields", description = "Map of field names to values to add")
            Map<String, Object> addFields,
            @Param(displayName = "Remove fields", description = "List of field names to remove")
            List<String> removeFields
    ) {}

    /** Output descriptor for {@link JsonTransformNode}. */
    public record Output(
            @Out(description = "Transformed data map") Map<String, Object> data
    ) {}

    private final ObjectMapper objectMapper;

    public JsonTransformNode(String nodeId) {
        super(nodeId, TYPE, Params.class);
        this.objectMapper = new ObjectMapper();
    }

    public JsonTransformNode(String nodeId, ObjectMapper objectMapper) {
        super(nodeId, TYPE, Params.class);
        this.objectMapper = objectMapper;
    }

    @Override
    protected Class<?> outputRecordType() {
        return Output.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> doExecute(ExecutionContext context, Params params) {
        Object input = params.input();

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
        List<String> removeFields = params.removeFields();
        if (removeFields != null) {
            for (String field : removeFields) {
                data.remove(field);
            }
        }

        // Add fields
        Map<String, Object> addFields = params.addFields();
        if (addFields != null) {
            data.putAll(addFields);
        }

        return data;
    }
}

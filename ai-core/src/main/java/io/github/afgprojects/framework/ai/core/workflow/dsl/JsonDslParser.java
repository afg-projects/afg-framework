package io.github.afgprojects.framework.ai.core.workflow.dsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition.NodeInstance;
import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition.Position;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;

import java.util.*;

/**
 * Parses JSON into WorkflowDefinition using Jackson.
 */
public class JsonDslParser {

    private final ObjectMapper objectMapper;

    public JsonDslParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WorkflowDefinition parse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String version = root.path("version").asText("1.0");

            // Parse nodes
            List<NodeInstance> nodes = new ArrayList<>();
            JsonNode nodesArray = root.path("nodes");
            if (nodesArray.isArray()) {
                for (JsonNode nodeJson : nodesArray) {
                    String id = nodeJson.path("id").asText();
                    String type = nodeJson.path("type").asText();
                    String name = nodeJson.path("name").asText();

                    JsonNode posJson = nodeJson.path("position");
                    Position position = new Position(
                        posJson.path("x").asDouble(0),
                        posJson.path("y").asDouble(0)
                    );

                    // Support both 'params' (legacy) and 'data' (React Flow style) for node parameters.
                    // When 'data' contains a nested 'data' key (React Flow wrapper: {name, data: {...}}),
                    // extract the inner data as the actual node params.
                    Map<String, Object> params = parseParams(nodeJson.path("params"));
                    if (params.isEmpty() && nodeJson.has("data")) {
                        params = parseParams(nodeJson.path("data"));
                        // Detect React Flow wrapper nesting: if 'data' field contains a 'data' sub-key,
                        // the actual node config is nested inside (e.g. { name: "Chat", data: { model: "gpt-4" } })
                        if (params.containsKey("data") && params.get("data") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> innerData = (Map<String, Object>) params.get("data");
                            params = innerData;
                        }
                    }

                    nodes.add(new NodeInstance(id, type, name, position, params));
                }
            }

            // Parse edges
            List<EdgeDefinition> edges = new ArrayList<>();
            JsonNode edgesArray = root.path("edges");
            if (edgesArray.isArray()) {
                for (JsonNode edgeJson : edgesArray) {
                    String id = edgeJson.path("id").asText();
                    String source = edgeJson.path("source").asText();
                    String target = edgeJson.path("target").asText();
                    String sourceAnchor = edgeJson.has("sourceAnchor")
                        ? edgeJson.path("sourceAnchor").asText()
                        : null; // Let EdgeDefinition compact constructor default to "output"

                    edges.add(new EdgeDefinition(id, source, target, sourceAnchor));
                }
            }

            return new WorkflowDefinition(version, nodes, edges);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.PARAM_FORMAT_ERROR, "Failed to parse workflow JSON: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseParams(JsonNode paramsNode) {
        if (paramsNode == null || !paramsNode.isObject()) {
            return Map.of();
        }
        try {
            return objectMapper.treeToValue(paramsNode, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}

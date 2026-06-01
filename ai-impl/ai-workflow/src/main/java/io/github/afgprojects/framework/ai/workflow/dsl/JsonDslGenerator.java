package io.github.afgprojects.framework.ai.workflow.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.afgprojects.framework.ai.core.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition.NodeInstance;

/**
 * Serializes WorkflowDefinition to JSON using Jackson with pretty print.
 */
public class JsonDslGenerator {

    private final ObjectMapper objectMapper;

    public JsonDslGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String generate(WorkflowDefinition workflow) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("version", workflow.version());

            // Serialize nodes
            ArrayNode nodesArray = root.putArray("nodes");
            for (NodeInstance node : workflow.nodes()) {
                ObjectNode nodeObj = nodesArray.addObject();
                nodeObj.put("id", node.id());
                nodeObj.put("type", node.type());
                nodeObj.put("name", node.name());

                ObjectNode posObj = nodeObj.putObject("position");
                posObj.put("x", node.position().x());
                posObj.put("y", node.position().y());

                // Use 'data' key to align with frontend WorkflowNodeDSL type (React Flow convention)
                nodeObj.set("data", objectMapper.valueToTree(node.params()));
            }

            // Serialize edges
            ArrayNode edgesArray = root.putArray("edges");
            for (EdgeDefinition edge : workflow.edges()) {
                ObjectNode edgeObj = edgesArray.addObject();
                edgeObj.put("id", edge.id());
                edgeObj.put("source", edge.source());
                edgeObj.put("target", edge.target());
                edgeObj.put("sourceAnchor", edge.sourceAnchor());
            }

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate workflow JSON: " + e.getMessage(), e);
        }
    }
}

package io.github.afgprojects.framework.ai.workflow.dsl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.workflow.definition.EdgeDefinition;
import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition;
import io.github.afgprojects.framework.ai.core.workflow.dsl.DslValidator;

import java.util.HashSet;
import java.util.Set;

/**
 * Default implementation of DslValidator.
 * Validates:
 * - At least one node
 * - Node IDs are unique
 * - Edge references exist
 * - Exactly one start node
 */
public class DefaultDslValidator implements DslValidator {

    private final JsonDslParser parser;

    public DefaultDslValidator() {
        this(new ObjectMapper());
    }

    public DefaultDslValidator(ObjectMapper objectMapper) {
        this.parser = new JsonDslParser(objectMapper);
    }

    @Override
    public void validate(WorkflowDefinition workflow) {
        // Check at least one node
        if (workflow.nodes() == null || workflow.nodes().isEmpty()) {
            throw new IllegalArgumentException("Workflow must have at least one node");
        }

        // Check node IDs are unique
        Set<String> nodeIds = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (var node : workflow.nodes()) {
            if (!nodeIds.add(node.id())) {
                duplicates.add(node.id());
            }
        }
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Duplicate node IDs: " + duplicates);
        }

        // Check edge references exist
        if (workflow.edges() != null) {
            for (EdgeDefinition edge : workflow.edges()) {
                if (!nodeIds.contains(edge.source())) {
                    throw new IllegalArgumentException(
                        "Edge '" + edge.id() + "' references non-existent source node: " + edge.source());
                }
                if (!nodeIds.contains(edge.target())) {
                    throw new IllegalArgumentException(
                        "Edge '" + edge.id() + "' references non-existent target node: " + edge.target());
                }
            }
        }

        // Check exactly one start node
        long startCount = workflow.nodes().stream()
            .filter(n -> "start".equals(n.type()))
            .count();
        if (startCount != 1) {
            throw new IllegalArgumentException(
                "Workflow must have exactly one start node, found: " + startCount);
        }
    }

    @Override
    public void validate(String json) {
        WorkflowDefinition workflow = parser.parse(json);
        validate(workflow);
    }
}

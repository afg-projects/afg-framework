package io.github.afgprojects.framework.ai.core.api.workflow.definition;

import java.util.List;
import java.util.Map;

public record WorkflowDefinition(
    String version,
    List<NodeInstance> nodes,
    List<EdgeDefinition> edges
) {
    public record NodeInstance(
        String id,
        String type,
        String name,
        Position position,
        Map<String, Object> params
    ) {}

    public record Position(double x, double y) {}
}

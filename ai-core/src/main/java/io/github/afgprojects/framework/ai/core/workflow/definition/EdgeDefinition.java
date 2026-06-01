package io.github.afgprojects.framework.ai.core.workflow.definition;

public record EdgeDefinition(
    String id,
    String source,
    String target,
    String sourceAnchor
) {
    public EdgeDefinition {
        if (sourceAnchor == null) {
            sourceAnchor = "output";
        }
    }
}

package io.github.afgprojects.framework.ai.core.api.workflow.definition;

import java.util.Map;
import java.util.Set;

public interface NodeDefinition {
    String getType();
    String getDisplayName();
    String getCategory();
    Map<String, ParamSchema> getParamSchema();
    Map<String, OutputSchema> getOutputSchema();

    default Set<String> getSourceAnchors() {
        return Set.of("output");
    }

    default Set<String> getTargetAnchors() {
        return Set.of("input");
    }
}

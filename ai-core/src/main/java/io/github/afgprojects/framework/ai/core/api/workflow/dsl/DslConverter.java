package io.github.afgprojects.framework.ai.core.api.workflow.dsl;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;

public interface DslConverter {
    String toJson(WorkflowDefinition workflow);
    WorkflowDefinition fromJson(String json);
}

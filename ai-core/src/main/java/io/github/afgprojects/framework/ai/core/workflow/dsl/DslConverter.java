package io.github.afgprojects.framework.ai.core.workflow.dsl;

import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition;

public interface DslConverter {
    String toJson(WorkflowDefinition workflow);
    WorkflowDefinition fromJson(String json);
}

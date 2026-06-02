package io.github.afgprojects.framework.ai.core.api.workflow.dsl;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;

public interface DslValidator {
    void validate(WorkflowDefinition workflow);
    void validate(String json);
}

package io.github.afgprojects.framework.ai.core.workflow.dsl;

import io.github.afgprojects.framework.ai.core.workflow.definition.WorkflowDefinition;

public interface DslValidator {
    void validate(WorkflowDefinition workflow);
    void validate(String json);
}

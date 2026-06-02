package io.github.afgprojects.framework.ai.core.api.workflow.engine;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.WorkflowDefinition;
import reactor.core.publisher.Flux;

public interface DagEngine {
    DagResult execute(WorkflowDefinition workflow, ExecutionContext context);
    Flux<DagEvent> executeStream(WorkflowDefinition workflow, ExecutionContext context);
}

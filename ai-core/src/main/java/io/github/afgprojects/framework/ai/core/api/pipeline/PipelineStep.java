package io.github.afgprojects.framework.ai.core.api.pipeline;

public interface PipelineStep {
    String getName();
    int getOrder();
    StepResult execute(PipelineContext context);
}

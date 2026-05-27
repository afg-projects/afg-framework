package io.github.afgprojects.framework.ai.core.pipeline;

public interface PipelineStep {
    String getName();
    int getOrder();
    StepResult execute(PipelineContext context);
}

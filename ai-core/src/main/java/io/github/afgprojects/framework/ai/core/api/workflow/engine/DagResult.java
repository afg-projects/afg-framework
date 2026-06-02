package io.github.afgprojects.framework.ai.core.api.workflow.engine;

import java.util.Map;

public record DagResult(
    String content,
    Map<String, NodeOutput> nodeOutputs,
    long totalTokenInput,
    long totalTokenOutput,
    long durationMs,
    DagStatus status
) {}

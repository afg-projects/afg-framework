package io.github.afgprojects.framework.ai.core.api.pipeline;

public record TokenUsage(
    long promptTokens,
    long completionTokens,
    long totalTokens
) {}

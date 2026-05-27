package io.github.afgprojects.framework.ai.core.pipeline;

public record TokenUsage(
    long promptTokens,
    long completionTokens,
    long totalTokens
) {}

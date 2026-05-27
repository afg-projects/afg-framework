package io.github.afgprojects.framework.ai.core.pipeline;

import java.util.List;
import java.util.Map;

public record PipelineResult(
    String content,
    String conversationId,
    List<SourceReference> sources,
    TokenUsage tokenUsage,
    String optimizedQuestion,
    long durationMs,
    Map<String, Object> metadata
) {}

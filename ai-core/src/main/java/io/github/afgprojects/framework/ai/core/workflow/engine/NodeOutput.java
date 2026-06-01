package io.github.afgprojects.framework.ai.core.workflow.engine;

import java.util.Map;

public record NodeOutput(
    Map<String, Object> data,
    String anchor,
    long tokenInput,
    long tokenOutput,
    long durationMs
) {
    public static NodeOutput of(Map<String, Object> data) {
        return new NodeOutput(data, "output", 0, 0, 0);
    }

    public static NodeOutput of(Map<String, Object> data, String anchor) {
        return new NodeOutput(data, anchor, 0, 0, 0);
    }

    public NodeOutput withDuration(long durationMs) {
        return new NodeOutput(data, anchor, tokenInput, tokenOutput, durationMs);
    }

    public NodeOutput withTokenUsage(long tokenInput, long tokenOutput) {
        return new NodeOutput(data, anchor, tokenInput, tokenOutput, durationMs);
    }
}

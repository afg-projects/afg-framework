package io.github.afgprojects.framework.ai.core.pipeline;

import java.util.Map;

public record StepResult(
    boolean skipped,
    String skipReason,
    Map<String, Object> outputVariables
) {
    public static StepResult ok(Map<String, Object> vars) {
        return new StepResult(false, null, vars);
    }

    public static StepResult skip(String reason) {
        return new StepResult(true, reason, Map.of());
    }
}

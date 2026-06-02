package io.github.afgprojects.framework.ai.core.api.multiagent.decomposition;

import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/**
 * 分解上下文
 */
public record DecompositionContext(
        @NonNull String workflowId,
        @NonNull List<?> availableAgents,
        @NonNull Map<String, Object> workflowConfig,
        int maxSubTasks
) {
    /**
     * 创建默认上下文
     */
    public static DecompositionContext of(String workflowId) {
        return new DecompositionContext(workflowId, List.of(), Map.of(), 10);
    }
}

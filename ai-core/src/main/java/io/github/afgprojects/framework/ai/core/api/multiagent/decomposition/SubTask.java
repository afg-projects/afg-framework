package io.github.afgprojects.framework.ai.core.api.multiagent.decomposition;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;

/**
 * 子任务
 */
public record SubTask(
        @NonNull String taskId,
        @NonNull String name,
        @NonNull String description,
        @NonNull TaskType type,
        @Nullable String suggestedAgent,
        @NonNull Map<String, Object> parameters,
        int priority,
        @Nullable Duration estimatedDuration
) {
    /**
     * 任务类型枚举
     */
    public enum TaskType {
        ANALYSIS,    // 分析任务
        PLANNING,    // 规划任务
        EXECUTION,   // 执行任务
        REVIEW       // 审核任务
    }

    /**
     * 创建简单子任务
     */
    public static SubTask of(String taskId, String name, String description, TaskType type) {
        return new SubTask(taskId, name, description, type, null, Map.of(), 0, null);
    }
}

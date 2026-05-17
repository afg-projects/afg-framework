package io.github.afgprojects.framework.ai.core.multiagent.decomposition;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 分解结果
 */
public record DecompositionResult(
        @NonNull List<SubTask> subTasks,
        @NonNull Map<String, List<String>> dependencies,
        @Nullable String strategy,
        @Nullable String decompositionReason
) {
    /**
     * 创建单任务结果
     */
    public static DecompositionResult singleTask(TaskDescription task) {
        SubTask subTask = SubTask.of(
                "task-1",
                task.name(),
                task.description(),
                SubTask.TaskType.EXECUTION
        );
        return new DecompositionResult(List.of(subTask), Map.of(), "single", null);
    }

    /**
     * 创建空结果
     */
    public static DecompositionResult empty() {
        return new DecompositionResult(List.of(), Map.of(), "none", null);
    }
}

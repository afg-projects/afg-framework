package io.github.afgprojects.framework.ai.core.api.multiagent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 任务分解结果
 *
 * <p>记录将复杂任务分解为子任务的结果。
 *
 * @param taskId       任务 ID
 * @param description  任务描述
 * @param subtasks     子任务列表
 * @param dependencies 子任务间的依赖关系
 * @param strategy     分解策略
 * @param metadata     元数据
 * @author afg-projects
 * @since 1.0.0
 */
public record TaskDecomposition(
        @NonNull String taskId,
        @NonNull String description,
        @NonNull List<SubTask> subtasks,
        @NonNull Map<String, List<String>> dependencies,
        @NonNull DecompositionStrategy strategy,
        @Nullable Map<String, Object> metadata
) {

    /**
     * 创建简单的任务分解
     *
     * @param taskId      任务 ID
     * @param description 任务描述
     * @param subtasks    子任务列表
     * @return 任务分解结果
     */
    public static @NonNull TaskDecomposition simple(
            @NonNull String taskId,
            @NonNull String description,
            @NonNull List<SubTask> subtasks
    ) {
        return new TaskDecomposition(taskId, description, subtasks, Map.of(), DecompositionStrategy.SEQUENTIAL, null);
    }

    /**
     * 创建带依赖的任务分解
     *
     * @param taskId       任务 ID
     * @param description  任务描述
     * @param subtasks     子任务列表
     * @param dependencies 依赖关系
     * @return 任务分解结果
     */
    public static @NonNull TaskDecomposition withDependencies(
            @NonNull String taskId,
            @NonNull String description,
            @NonNull List<SubTask> subtasks,
            @NonNull Map<String, List<String>> dependencies
    ) {
        return new TaskDecomposition(taskId, description, subtasks, dependencies, DecompositionStrategy.DAG, null);
    }

    /**
     * 子任务
     *
     * @param subtaskId    子任务 ID
     * @param description  子任务描述
     * @param assignedAgent 分配的 Agent ID
     * @param priority     优先级
     * @param input        输入参数
     */
    public record SubTask(
            @NonNull String subtaskId,
            @NonNull String description,
            @Nullable String assignedAgent,
            int priority,
            @Nullable Map<String, Object> input
    ) {
        /**
         * 创建简单子任务
         */
        public static @NonNull SubTask simple(
                @NonNull String subtaskId,
                @NonNull String description
        ) {
            return new SubTask(subtaskId, description, null, 0, null);
        }

        /**
         * 创建分配给指定 Agent 的子任务
         */
        public static @NonNull SubTask assigned(
                @NonNull String subtaskId,
                @NonNull String description,
                @NonNull String assignedAgent
        ) {
            return new SubTask(subtaskId, description, assignedAgent, 0, null);
        }

        /**
         * 创建带优先级的子任务
         */
        public static @NonNull SubTask withPriority(
                @NonNull String subtaskId,
                @NonNull String description,
                int priority
        ) {
            return new SubTask(subtaskId, description, null, priority, null);
        }
    }

    /**
     * 分解策略
     */
    public enum DecompositionStrategy {
        /**
         * 顺序执行
         */
        SEQUENTIAL,
        /**
         * 并行执行
         */
        PARALLEL,
        /**
         * 有向无环图
         */
        DAG,
        /**
         * 层次化
         */
        HIERARCHICAL
    }
}

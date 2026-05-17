package io.github.afgprojects.framework.ai.core.multiagent.decomposition;

import org.jspecify.annotations.NonNull;

/**
 * 任务分解器接口
 */
public interface TaskDecomposer {

    /**
     * 分解任务
     */
    @NonNull
    DecompositionResult decompose(@NonNull TaskDescription task, @NonNull DecompositionContext context);

    /**
     * 分解器名称
     */
    @NonNull
    String getName();

    /**
     * 是否支持该任务
     */
    boolean supports(@NonNull TaskDescription task);
}

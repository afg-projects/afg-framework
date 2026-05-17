package io.github.afgprojects.framework.ai.core.multiagent.graph;

import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;

/**
 * 边条件接口
 *
 * <p>用于条件分支，根据工作流状态决定下一个节点。
 */
@FunctionalInterface
public interface EdgeCondition {

    /**
     * 评估条件
     *
     * @param state 当前状态
     * @return 条件结果，用于匹配 branches 映射
     */
    @NonNull
    String evaluate(@NonNull WorkflowState state);
}

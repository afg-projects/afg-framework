package io.github.afgprojects.framework.ai.core.multiagent.node;

import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * 路由条件
 *
 * @param targetNodeId 目标节点ID
 * @param condition    条件判断
 * @param description  描述
 */
public record RouteCondition(
        @NonNull String targetNodeId,
        @NonNull Predicate<WorkflowState> condition,
        @Nullable String description
) {
    /**
     * 创建路由条件
     */
    public static RouteCondition of(String targetNodeId, Predicate<WorkflowState> condition) {
        return new RouteCondition(targetNodeId, condition, null);
    }

    /**
     * 创建带描述的路由条件
     */
    public static RouteCondition of(String targetNodeId, Predicate<WorkflowState> condition, String description) {
        return new RouteCondition(targetNodeId, condition, description);
    }
}

package io.github.afgprojects.framework.ai.core.multiagent.graph;

import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * 节点执行结果
 */
public record NodeResult(
        @NonNull NodeStatus status,
        @NonNull WorkflowState updatedState,
        @Nullable String nextNodeId,
        @Nullable List<String> parallelNodes,
        @Nullable String errorMessage,
        @Nullable Duration executionDuration
) {
    /**
     * 创建成功结果
     */
    public static NodeResult success(WorkflowState state) {
        return new NodeResult(NodeStatus.SUCCESS, state, null, null, null, null);
    }

    /**
     * 创建成功结果（带执行时间）
     */
    public static NodeResult success(WorkflowState state, Duration duration) {
        return new NodeResult(NodeStatus.SUCCESS, state, null, null, null, duration);
    }

    /**
     * 创建失败结果
     */
    public static NodeResult failure(WorkflowState state, String error) {
        return new NodeResult(NodeStatus.FAILURE, state, null, null, error, null);
    }

    /**
     * 创建路由结果
     */
    public static NodeResult routeTo(WorkflowState state, String nodeId) {
        return new NodeResult(NodeStatus.SUCCESS, state, nodeId, null, null, null);
    }

    /**
     * 创建并行执行结果
     */
    public static NodeResult parallel(WorkflowState state, List<String> nodeIds) {
        return new NodeResult(NodeStatus.PARALLEL_WAIT, state, null, nodeIds, null, null);
    }

    /**
     * 创建需要输入结果
     */
    public static NodeResult needsInput(WorkflowState state) {
        return new NodeResult(NodeStatus.NEEDS_INPUT, state, null, null, null, null);
    }
}

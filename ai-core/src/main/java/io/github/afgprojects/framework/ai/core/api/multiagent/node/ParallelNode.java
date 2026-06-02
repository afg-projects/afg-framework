package io.github.afgprojects.framework.ai.core.api.multiagent.node;

import io.github.afgprojects.framework.ai.core.api.workflow.engine.WorkflowNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.ExecutionContext;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 并行执行节点 - 同时执行多个子节点。
 * <p>
 * 在 DAG 工作流中，并行节点将任务分发到多个子节点同时执行，
 * 等待所有子节点完成后合并结果。
 */
public interface ParallelNode extends WorkflowNode {

    /**
     * 获取并行执行的子节点
     */
    List<WorkflowNode> getChildren();

    /**
     * 获取合并策略
     */
    @Nullable
    default MergeStrategy getMergeStrategy() {
        return MergeStrategy.ALL;
    }

    /**
     * 结果合并策略
     */
    enum MergeStrategy {
        /** 等待所有子节点完成 */
        ALL,
        /** 任一子节点完成即返回 */
        ANY,
        /** 仅返回成功的结果 */
        SUCCESS_ONLY
    }
}
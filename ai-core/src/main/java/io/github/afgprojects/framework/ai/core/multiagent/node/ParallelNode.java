package io.github.afgprojects.framework.ai.core.multiagent.node;

import io.github.afgprojects.framework.ai.core.multiagent.graph.Node;
import io.github.afgprojects.framework.ai.core.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 并行执行节点接口
 *
 * <p>定义并行执行多个子节点的行为，支持不同的并行策略和失败处理方式。
 */
public interface ParallelNode extends Node {

    /**
     * 并行执行的子节点ID列表
     *
     * @return 子节点ID列表（不可变）
     */
    @NonNull
    List<String> getParallelNodes();

    /**
     * 并行策略
     *
     * @return 并行执行策略
     */
    @NonNull
    ParallelStrategy getStrategy();

    /**
     * 结果聚合器
     *
     * @return 用于聚合多个子节点结果的聚合器
     */
    @NonNull
    ResultAggregator getAggregator();

    /**
     * 失败处理
     *
     * @return 失败时的处理策略
     */
    @NonNull
    FailureHandling getFailureHandling();

    /**
     * 并行策略枚举
     */
    enum ParallelStrategy {
        /**
         * 所有节点都执行，等待所有完成
         */
        ALL,

        /**
         * 任一成功即返回
         */
        ANY,

        /**
         * M 个节点中至少 N 个成功
         */
        N_OF_M
    }

    /**
     * 失败处理枚举
     */
    enum FailureHandling {
        /**
         * 继续执行其他节点
         */
        CONTINUE,

        /**
         * 立即终止所有并行执行
         */
        ABORT,

        /**
         * 重试失败节点
         */
        RETRY
    }

    /**
     * 结果聚合器
     *
     * <p>用于将多个子节点的执行结果聚合为最终状态。
     */
    @FunctionalInterface
    interface ResultAggregator {
        /**
         * 聚合多个节点结果
         *
         * @param results 所有子节点的执行结果
         * @param state   当前工作流状态
         * @return 聚合后的工作流状态
         */
        WorkflowState aggregate(List<NodeResult> results, WorkflowState state);
    }
}

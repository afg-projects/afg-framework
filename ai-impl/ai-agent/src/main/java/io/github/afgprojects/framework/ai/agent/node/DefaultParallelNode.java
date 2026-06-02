package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.ParallelNode;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * 默认并行节点实现
 *
 * <p>此节点只返回并行节点列表，实际并行执行由 GraphExecution 处理。
 * 支持多种并行策略和失败处理方式。
 */
public class DefaultParallelNode implements ParallelNode {

    private final String id;
    private final List<String> parallelNodes;
    private final ParallelStrategy strategy;
    private final ResultAggregator aggregator;
    private final FailureHandling failureHandling;

    /**
     * 创建并行节点
     *
     * @param id              节点ID
     * @param parallelNodes   并行执行的子节点ID列表
     * @param strategy        并行策略
     * @param aggregator      结果聚合器
     * @param failureHandling 失败处理策略
     */
    public DefaultParallelNode(
            String id,
            List<String> parallelNodes,
            ParallelStrategy strategy,
            ResultAggregator aggregator,
            FailureHandling failureHandling
    ) {
        this.id = id;
        this.parallelNodes = List.copyOf(parallelNodes);
        this.strategy = strategy;
        this.aggregator = aggregator;
        this.failureHandling = failureHandling;
    }

    @Override
    @NonNull
    public String getId() {
        return id;
    }

    @Override
    @NonNull
    public NodeResult execute(@NonNull WorkflowState state) {
        // 返回并行节点列表，由执行器处理实际并行执行
        return NodeResult.parallel(state, parallelNodes);
    }

    @Override
    @NonNull
    public NodeType getType() {
        return NodeType.PARALLEL;
    }

    @Override
    @NonNull
    public List<String> getParallelNodes() {
        return parallelNodes;
    }

    @Override
    @NonNull
    public ParallelStrategy getStrategy() {
        return strategy;
    }

    @Override
    @NonNull
    public ResultAggregator getAggregator() {
        return aggregator;
    }

    @Override
    @NonNull
    public FailureHandling getFailureHandling() {
        return failureHandling;
    }
}

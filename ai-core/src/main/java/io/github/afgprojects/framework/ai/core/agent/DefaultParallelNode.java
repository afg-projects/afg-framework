package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.node.ParallelNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 默认并行节点实现
 *
 * <p>并行节点将任务分发到多个子节点同时执行，
 * 等待所有子节点完成后合并结果。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultParallelNode implements ParallelNode {

    private final String nodeId;
    private final List<WorkflowNode> children;
    private final MergeStrategy mergeStrategy;

    /**
     * 创建并行节点
     *
     * @param nodeId        节点 ID
     * @param children      子节点列表
     * @param mergeStrategy 合并策略
     */
    public DefaultParallelNode(@NonNull String nodeId, @NonNull List<WorkflowNode> children,
                               @Nullable MergeStrategy mergeStrategy) {
        this.nodeId = nodeId;
        this.children = List.copyOf(children);
        this.mergeStrategy = mergeStrategy != null ? mergeStrategy : MergeStrategy.ALL;
    }

    /**
     * 创建并行节点（默认 ALL 合并策略）
     *
     * @param nodeId   节点 ID
     * @param children 子节点列表
     */
    public DefaultParallelNode(@NonNull String nodeId, @NonNull List<WorkflowNode> children) {
        this(nodeId, children, MergeStrategy.ALL);
    }

    @Override
    @NonNull
    public String getNodeId() {
        return nodeId;
    }

    @Override
    @NonNull
    public String getType() {
        return "parallel";
    }

    @Override
    @NonNull
    public NodeOutput execute(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        // 并行执行所有子节点
        List<NodeOutput> outputs = children.stream()
                .map(child -> child.execute(context, params))
                .toList();

        // 合并结果
        Map<String, Object> mergedData = new java.util.LinkedHashMap<>();
        for (NodeOutput output : outputs) {
            if (output.data() != null) {
                mergedData.putAll(output.data());
            }
        }

        return NodeOutput.of(mergedData, nodeId);
    }

    @Override
    @NonNull
    public Flux<WorkflowNode.NodeEvent> executeStream(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        // 并行流式执行所有子节点
        List<Flux<WorkflowNode.NodeEvent>> streams = children.stream()
                .map(child -> child.executeStream(context, params))
                .toList();

        return Flux.merge(streams);
    }

    @Override
    @NonNull
    public List<WorkflowNode> getChildren() {
        return children;
    }

    @Override
    @NonNull
    public MergeStrategy getMergeStrategy() {
        return mergeStrategy;
    }
}
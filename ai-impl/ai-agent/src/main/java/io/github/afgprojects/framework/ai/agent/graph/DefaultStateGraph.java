package io.github.afgprojects.framework.ai.agent.graph;

import io.github.afgprojects.framework.ai.agent.state.InMemoryStateManager;
import io.github.afgprojects.framework.ai.core.multiagent.graph.*;
import io.github.afgprojects.framework.ai.core.multiagent.state.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认状态图实现
 *
 * <p>提供基于 DAG 的工作流执行引擎，支持条件分支和状态管理。
 */
public class DefaultStateGraph implements StateGraph {

    private static final Logger log = LoggerFactory.getLogger(DefaultStateGraph.class);

    private final String name;
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();
    private final Map<String, String> edges = new ConcurrentHashMap<>();
    private final Map<String, ConditionalEdge> conditionalEdges = new ConcurrentHashMap<>();
    private final Set<String> finishPoints = ConcurrentHashMap.newKeySet();
    private final StateManager stateManager;

    private String entryPoint;

    /**
     * 创建状态图（使用内存状态管理器）
     */
    public DefaultStateGraph(String name) {
        this(name, new InMemoryStateManager());
    }

    /**
     * 创建状态图
     */
    public DefaultStateGraph(String name, StateManager stateManager) {
        this.name = name;
        this.stateManager = stateManager;
    }

    @Override
    @NonNull
    public StateGraph addNode(@NonNull String id, @NonNull Node node) {
        log.debug("Adding node: {}", id);
        nodes.put(id, node);
        return this;
    }

    @Override
    @NonNull
    public StateGraph addEdge(@NonNull String from, @NonNull String to) {
        log.debug("Adding edge: {} -> {}", from, to);
        edges.put(from, to);
        return this;
    }

    @Override
    @NonNull
    public StateGraph addConditionalEdge(@NonNull String from, @NonNull EdgeCondition condition, @NonNull Map<String, String> branches) {
        log.debug("Adding conditional edge from: {}", from);
        conditionalEdges.put(from, new ConditionalEdge(condition, branches));
        return this;
    }

    @Override
    @NonNull
    public StateGraph setEntryPoint(@NonNull String nodeId) {
        log.debug("Setting entry point: {}", nodeId);
        this.entryPoint = nodeId;
        return this;
    }

    @Override
    @NonNull
    public StateGraph setFinishPoint(@NonNull String nodeId) {
        log.debug("Adding finish point: {}", nodeId);
        finishPoints.add(nodeId);
        return this;
    }

    @Override
    @NonNull
    public WorkflowState execute(@NonNull WorkflowInput input) {
        if (entryPoint == null) {
            throw new IllegalStateException("Entry point not set");
        }

        String workflowId = UUID.randomUUID().toString();
        log.info("Starting workflow: {}, graph: {}", workflowId, name);

        WorkflowState state = stateManager.createState(workflowId, name, input);

        String currentNodeId = entryPoint;
        int maxIterations = nodes.size() * 10; // 防止无限循环
        int iterations = 0;

        while (currentNodeId != null && iterations < maxIterations) {
            iterations++;

            Node node = nodes.get(currentNodeId);
            if (node == null) {
                throw new IllegalStateException("Node not found: " + currentNodeId);
            }

            log.debug("Executing node: {}", currentNodeId);
            state = state.withCurrentNode(currentNodeId);

            NodeResult result = node.execute(state);
            state = result.updatedState();

            // 保存节点输出到状态
            state = state.with("_nodeOutputs", mergeNodeOutputs(state, currentNodeId, result));

            // 保存状态
            state = stateManager.updateState(workflowId, state);

            // 检查是否完成
            if (finishPoints.contains(currentNodeId)) {
                log.info("Workflow completed at finish point: {}", currentNodeId);
                return state.with("_status", WorkflowStatus.COMPLETED.name());
            }

            // 确定下一个节点
            currentNodeId = getNextNode(currentNodeId, result, state);
        }

        if (iterations >= maxIterations) {
            log.error("Workflow exceeded max iterations: {}", maxIterations);
            return state.with("_status", WorkflowStatus.FAILED.name());
        }

        return state.with("_status", WorkflowStatus.COMPLETED.name());
    }

    @Override
    @NonNull
    public WorkflowState resumeFromCheckpoint(@NonNull String checkpointId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeNodeOutputs(WorkflowState state, String nodeId, NodeResult result) {
        Map<String, Object> existingOutputs = state.get("_nodeOutputs", new HashMap<>());
        Map<String, Object> outputs = new HashMap<>(existingOutputs);
        outputs.put(nodeId, result.updatedState().getData());
        return outputs;
    }

    private String getNextNode(String currentNodeId, NodeResult result, WorkflowState state) {
        // 优先使用结果中指定的节点
        if (result.nextNodeId() != null) {
            return result.nextNodeId();
        }

        // 检查条件边
        ConditionalEdge conditionalEdge = conditionalEdges.get(currentNodeId);
        if (conditionalEdge != null) {
            String branch = conditionalEdge.condition().evaluate(state);
            String nextNode = conditionalEdge.branches().get(branch);
            if (nextNode != null) {
                return nextNode;
            }
            log.warn("No branch found for condition result: {}", branch);
            return null;
        }

        // 使用普通边
        return edges.get(currentNodeId);
    }

    /**
     * 条件边
     */
    private record ConditionalEdge(EdgeCondition condition, Map<String, String> branches) {}
}

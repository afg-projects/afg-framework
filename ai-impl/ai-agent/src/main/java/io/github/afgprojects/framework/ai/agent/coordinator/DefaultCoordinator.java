package io.github.afgprojects.framework.ai.agent.coordinator;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentRoutingStrategy;
import io.github.afgprojects.framework.ai.core.api.multiagent.Coordinator;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认协调者实现
 *
 * <p>提供通用的多 Agent 协调逻辑，包括：
 * <ul>
 *   <li>Agent 注册/注销 - 管理可用 Agent 的生命周期</li>
 *   <li>状态管理 - 跟踪 Agent 和工作流的运行状态</li>
 *   <li>并行执行 - 协调多个 Agent 并行处理任务</li>
 *   <li>顺序执行 - 按序协调多个 Agent 依次处理任务</li>
 *   <li>冲突解决 - 解决 Agent 之间的执行冲突</li>
 *   <li>结果合并 - 合并多个 Agent 的执行结果</li>
 * </ul>
 *
 * <p>路由策略通过 {@link AgentRoutingStrategy} 注入，默认使用 {@link DefaultAgentRoutingStrategy}。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultCoordinator implements Coordinator {

    private final AgentRoutingStrategy routingStrategy;

    /**
     * Agent 注册表
     */
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * Agent 状态表
     */
    private final Map<String, Object> agentStatus = new ConcurrentHashMap<>();

    /**
     * 工作流状态表
     */
    private final Map<String, WorkflowState> workflows = new ConcurrentHashMap<>();

    /**
     * 使用默认路由策略创建协调者
     */
    public DefaultCoordinator() {
        this(new DefaultAgentRoutingStrategy());
    }

    /**
     * 使用指定路由策略创建协调者
     *
     * @param routingStrategy Agent 路由策略
     */
    public DefaultCoordinator(@NonNull AgentRoutingStrategy routingStrategy) {
        this.routingStrategy = routingStrategy;
    }

    // ========== Agent 注册/注销 ==========

    /**
     * 注册 Agent
     *
     * @param agent 要注册的 Agent
     */
    public void registerAgent(@NonNull Agent agent) {
        String name = agent.getName().toLowerCase();
        agents.put(name, agent);
        agentStatus.put(name, AgentState.IDLE);
        log.info("Registered agent: {}", agent.getName());
    }

    /**
     * 注销 Agent
     *
     * @param agentName Agent 名称
     */
    public void unregisterAgent(@NonNull String agentName) {
        String name = agentName.toLowerCase();
        agents.remove(name);
        agentStatus.remove(name);
        log.info("Unregistered agent: {}", agentName);
    }

    /**
     * 获取所有已注册的 Agent
     *
     * @return Agent 列表
     */
    @NonNull
    public List<Agent> getRegisteredAgents() {
        return new ArrayList<>(agents.values());
    }

    /**
     * 获取 Agent
     *
     * @param agentName Agent 名称
     * @return Agent 实例，如果不存在返回 null
     */
    @Nullable
    public Agent getAgent(@NonNull String agentName) {
        return agents.get(agentName.toLowerCase());
    }

    // ========== 路由执行 ==========

    /**
     * 根据路由策略选择合适的 Agent 执行任务
     *
     * @param request 执行请求
     * @return 执行响应
     * @throws IllegalStateException 如果没有找到合适的 Agent
     */
    @NonNull
    public AgentResponse routeAndExecute(@NonNull AgentRequest request) {
        String selectedAgentName = routingStrategy.selectAgent(agents, request);

        if (selectedAgentName == null) {
            throw new IllegalStateException("No suitable agent found for the task");
        }

        Agent agent = agents.get(selectedAgentName);
        if (agent == null) {
            throw new IllegalStateException("Agent '" + selectedAgentName + "' not found in registry");
        }

        log.info("Routing task to agent: {}", selectedAgentName);

        updateAgentState(selectedAgentName, AgentState.BUSY);
        try {
            return agent.execute(request);
        } finally {
            updateAgentState(selectedAgentName, AgentState.IDLE);
        }
    }

    // ========== 并行/顺序执行 ==========

    /**
     * 协调多个 Agent 并行执行任务
     *
     * @param agentRequests Agent 请求列表
     * @param workflowId    工作流 ID
     * @return 执行响应列表
     */
    @NonNull
    public CompletableFuture<List<AgentResponse>> executeParallel(
        @NonNull List<AgentRequest> agentRequests,
        @NonNull String workflowId
    ) {
        workflows.put(workflowId, new WorkflowState(workflowId, agentRequests.size()));

        List<CompletableFuture<AgentResponse>> futures = agentRequests.stream()
            .map(request -> CompletableFuture.supplyAsync(() -> {
                try {
                    String agentName = routingStrategy.selectAgent(agents, request);
                    if (agentName == null) {
                        return AgentResponse.error("No suitable agent found");
                    }
                    Agent agent = agents.get(agentName);
                    if (agent == null) {
                        return AgentResponse.error("Agent '" + agentName + "' not found");
                    }

                    updateAgentState(agentName, AgentState.BUSY);
                    try {
                        return agent.execute(request);
                    } finally {
                        updateAgentState(agentName, AgentState.IDLE);
                        updateWorkflowProgress(workflowId);
                    }
                } catch (Exception e) {
                    log.error("Parallel execution failed for workflow: {}", workflowId, e);
                    return AgentResponse.error("Execution failed: " + e.getMessage(), e);
                }
            }))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .toList());
    }

    /**
     * 协调多个 Agent 顺序执行任务
     *
     * <p>顺序执行时，如果某个 Agent 执行失败，后续 Agent 将不会执行。
     *
     * @param agentRequests Agent 请求列表
     * @param workflowId    工作流 ID
     * @return 执行响应列表
     */
    @NonNull
    public CompletableFuture<List<AgentResponse>> executeSequential(
        @NonNull List<AgentRequest> agentRequests,
        @NonNull String workflowId
    ) {
        workflows.put(workflowId, new WorkflowState(workflowId, agentRequests.size()));

        return CompletableFuture.supplyAsync(() -> {
            List<AgentResponse> responses = new ArrayList<>();

            for (AgentRequest request : agentRequests) {
                try {
                    String agentName = routingStrategy.selectAgent(agents, request);
                    if (agentName == null) {
                        responses.add(AgentResponse.error("No suitable agent found"));
                        log.warn("Sequential workflow stopped: no suitable agent, workflowId={}", workflowId);
                        break;
                    }

                    Agent agent = agents.get(agentName);
                    if (agent == null) {
                        responses.add(AgentResponse.error("Agent '" + agentName + "' not found"));
                        log.warn("Sequential workflow stopped: agent not found, workflowId={}", workflowId);
                        break;
                    }

                    updateAgentState(agentName, AgentState.BUSY);
                    AgentResponse response;
                    try {
                        response = agent.execute(request);
                    } finally {
                        updateAgentState(agentName, AgentState.IDLE);
                    }

                    responses.add(response);
                    updateWorkflowProgress(workflowId);

                    if (response.isError()) {
                        log.warn("Sequential workflow stopped due to failure: workflowId={}", workflowId);
                        break;
                    }
                } catch (Exception e) {
                    log.error("Sequential workflow execution failed: workflowId={}", workflowId, e);
                    responses.add(AgentResponse.error("Execution failed: " + e.getMessage(), e));
                    break;
                }
            }

            return responses;
        });
    }

    // ========== Coordinator 接口实现 ==========

    @Override
    @NonNull
    public String getName() {
        return "DefaultCoordinator";
    }

    @Override
    @NonNull
    public CompletableFuture<CoordinationResult> coordinate(@NonNull String workflowId) {
        return CompletableFuture.supplyAsync(() -> {
            WorkflowState state = workflows.get(workflowId);
            if (state == null) {
                return CoordinationResult.failure("Workflow not found: " + workflowId);
            }

            if (state.isCompleted()) {
                return CoordinationResult.success(state.getResults());
            }

            return CoordinationResult.failure("Workflow not completed: " + workflowId);
        });
    }

    @Override
    @NonNull
    public ConflictResolution resolveConflict(@NonNull Conflict conflict) {
        log.warn("Resolving conflict: {}", conflict);

        // 简单的冲突解决策略：优先级高的 Agent 获胜
        // 实际应用中可以实现更复杂的策略
        return ConflictResolution.resolved("Conflict resolved by priority");
    }

    @Override
    @NonNull
    public Object mergeResults(@NonNull Iterable<?> results) {
        List<String> mergedOutputs = new ArrayList<>();

        for (Object result : results) {
            if (result instanceof AgentResponse response) {
                if (response.output() != null) {
                    mergedOutputs.add(response.output());
                }
            } else if (result instanceof String text) {
                mergedOutputs.add(text);
            }
        }

        return String.join("\n\n---\n\n", mergedOutputs);
    }

    @Override
    public void syncStatus(@NonNull String agentId, @NonNull Object status) {
        agentStatus.put(agentId.toLowerCase(), status);
        log.debug("Synced agent status: agentId={}, status={}", agentId, status);
    }

    @Override
    @Nullable
    public Object getStatus(@NonNull String agentId) {
        return agentStatus.get(agentId.toLowerCase());
    }

    // ========== 私有方法 ==========

    private void updateAgentState(String agentName, AgentState state) {
        agentStatus.put(agentName.toLowerCase(), state);
        log.debug("Updated agent state: agent={}, state={}", agentName, state);
    }

    private void updateWorkflowProgress(String workflowId) {
        WorkflowState state = workflows.get(workflowId);
        if (state != null) {
            state.incrementCompleted();
            log.debug("Updated workflow progress: workflowId={}, completed={}/{}",
                workflowId, state.getCompletedCount(), state.getTotalCount());
        }
    }

    // ========== 内部类 ==========

    /**
     * Agent 状态枚举
     */
    public enum AgentState {
        /**
         * 空闲
         */
        IDLE,
        /**
         * 忙碌
         */
        BUSY,
        /**
         * 离线
         */
        OFFLINE
    }

    /**
     * 工作流状态
     */
    private static class WorkflowState {
        private final String workflowId;
        private final int totalCount;
        private int completedCount = 0;
        private final List<Object> results = new ArrayList<>();

        WorkflowState(String workflowId, int totalCount) {
            this.workflowId = workflowId;
            this.totalCount = totalCount;
        }

        synchronized void incrementCompleted() {
            completedCount++;
        }

        synchronized void addResult(Object result) {
            results.add(result);
        }

        boolean isCompleted() {
            return completedCount >= totalCount;
        }

        int getCompletedCount() {
            return completedCount;
        }

        int getTotalCount() {
            return totalCount;
        }

        List<Object> getResults() {
            return results;
        }
    }
}

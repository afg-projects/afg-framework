package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentWorkflow;
import io.github.afgprojects.framework.ai.core.api.multiagent.Orchestrator;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.Checkpoint;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.CheckpointPolicy;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.StateManager;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.WorkflowInput;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.WorkflowState;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认 Agent 工作流实现
 *
 * <p>基于 DAG（有向无环图）的工作流执行引擎，提供：
 * <ul>
 *   <li>拓扑排序 - 按依赖关系确定步骤执行顺序</li>
 *   <li>顺序执行 - 按拓扑序依次执行每个步骤</li>
 *   <li>暂停/恢复 - 支持工作流暂停后从检查点恢复</li>
 *   <li>取消 - 支持取消正在执行的工作流</li>
 *   <li>检查点 - 通过 {@link StateManager} 持久化执行进度</li>
 *   <li>输入/输出映射 - 步骤间的数据传递</li>
 * </ul>
 *
 * <p>每个步骤通过 {@link Orchestrator} 委托给对应的 Agent 执行。
 * 检查点策略由 {@link CheckpointPolicy} 控制，默认为 {@link CheckpointPolicy#EVERY_NODE}。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultAgentWorkflow implements AgentWorkflow {

    private final @NonNull String workflowId;
    private final @NonNull String name;
    private final @NonNull List<WorkflowStep> steps;
    private final @NonNull Orchestrator orchestrator;
    private final @Nullable StateManager stateManager;
    private final @NonNull CheckpointPolicy checkpointPolicy;

    private final AtomicReference<WorkflowState> state = new AtomicReference<>(WorkflowState.PENDING);
    private final Map<String, Object> stepResults = new ConcurrentHashMap<>();
    private final Map<String, AgentResponse> stepResponses = new ConcurrentHashMap<>();
    private volatile @Nullable String currentStepId;
    private volatile @Nullable CompletableFuture<WorkflowResult> executionFuture;

    /**
     * 创建默认工作流
     *
     * @param workflowId 工作流 ID
     * @param name       工作流名称
     * @param steps      工作流步骤
     * @param orchestrator 编排者
     */
    public DefaultAgentWorkflow(
            @NonNull String workflowId,
            @NonNull String name,
            @NonNull List<WorkflowStep> steps,
            @NonNull Orchestrator orchestrator
    ) {
        this(workflowId, name, steps, orchestrator, null, CheckpointPolicy.EVERY_NODE);
    }

    /**
     * 创建带状态管理器的默认工作流
     *
     * @param workflowId       工作流 ID
     * @param name             工作流名称
     * @param steps            工作流步骤
     * @param orchestrator     编排者
     * @param stateManager     状态管理器（可选）
     * @param checkpointPolicy 检查点策略
     */
    public DefaultAgentWorkflow(
            @NonNull String workflowId,
            @NonNull String name,
            @NonNull List<WorkflowStep> steps,
            @NonNull Orchestrator orchestrator,
            @Nullable StateManager stateManager,
            @NonNull CheckpointPolicy checkpointPolicy
    ) {
        this.workflowId = workflowId;
        this.name = name;
        this.steps = List.copyOf(steps);
        this.orchestrator = orchestrator;
        this.stateManager = stateManager;
        this.checkpointPolicy = checkpointPolicy;
        validateDag();
    }

    // ========== AgentWorkflow 接口实现 ==========

    @Override
    @NonNull
    public String getWorkflowId() {
        return workflowId;
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @NonNull
    public List<WorkflowStep> getSteps() {
        return steps;
    }

    @Override
    @NonNull
    public CompletableFuture<WorkflowResult> execute(@NonNull AgentRequest request) {
        WorkflowState currentState = state.get();
        if (currentState == WorkflowState.RUNNING) {
            log.warn("Workflow '{}' is already running", workflowId);
            CompletableFuture<WorkflowResult> existing = executionFuture;
            if (existing != null) {
                return existing;
            }
            return CompletableFuture.completedFuture(
                WorkflowResult.failure("Workflow is already running"));
        }

        if (currentState == WorkflowState.PAUSED) {
            log.info("Resuming paused workflow '{}' from step '{}'", workflowId, currentStepId);
            return resumeFromCheckpoint(request);
        }

        if (currentState == WorkflowState.COMPLETED || currentState == WorkflowState.CANCELLED) {
            log.warn("Workflow '{}' is already {} and cannot be re-executed", workflowId, currentState);
            return CompletableFuture.completedFuture(
                WorkflowResult.failure("Workflow is already " + currentState));
        }

        state.set(WorkflowState.RUNNING);
        log.info("Starting workflow '{}' with {} steps", workflowId, steps.size());

        executionFuture = CompletableFuture.supplyAsync(() -> executeWorkflow(request));
        return executionFuture;
    }

    @Override
    @NonNull
    public WorkflowState getState() {
        return state.get();
    }

    @Override
    public void pause() {
        WorkflowState previous = state.getAndUpdate(current ->
            current == WorkflowState.RUNNING ? WorkflowState.PAUSED : current
        );
        if (previous == WorkflowState.RUNNING) {
            log.info("Pausing workflow '{}' at step '{}'", workflowId, currentStepId);
            saveCheckpointIfNeeded();
        } else {
            log.warn("Cannot pause workflow '{}': current state is {}", workflowId, previous);
        }
    }

    @Override
    public void resume() {
        WorkflowState previous = state.getAndUpdate(current ->
            current == WorkflowState.PAUSED ? WorkflowState.RUNNING : current
        );
        if (previous == WorkflowState.PAUSED) {
            log.info("Resuming workflow '{}' from step '{}'", workflowId, currentStepId);
        } else {
            log.warn("Cannot resume workflow '{}': current state is {}", workflowId, previous);
        }
    }

    @Override
    public void cancel() {
        WorkflowState previous = state.getAndUpdate(current ->
            current == WorkflowState.RUNNING || current == WorkflowState.PAUSED
                ? WorkflowState.CANCELLED
                : current
        );
        if (previous == WorkflowState.RUNNING || previous == WorkflowState.PAUSED) {
            log.info("Cancelling workflow '{}' (was {})", workflowId, previous);
            CompletableFuture<WorkflowResult> future = executionFuture;
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        } else {
            log.warn("Cannot cancel workflow '{}': current state is {}", workflowId, previous);
        }
    }

    // ========== 工作流执行 ==========

    private WorkflowResult executeWorkflow(AgentRequest request) {
        List<String> sortedStepIds = topologicalSort();
        log.debug("Workflow '{}' execution order: {}", workflowId, sortedStepIds);

        Map<String, Object> contextData = new HashMap<>(request.context());
        contextData.put("userInput", request.userInput());
        contextData.put("sessionId", request.sessionId());

        AgentResponse finalResponse = null;

        for (String stepId : sortedStepIds) {
            // Check for cancellation
            if (state.get() == WorkflowState.CANCELLED) {
                log.info("Workflow '{}' cancelled at step '{}'", workflowId, stepId);
                return WorkflowResult.failure("Workflow cancelled at step: " + stepId);
            }

            // Wait while paused
            awaitWhilePaused();
            if (state.get() == WorkflowState.CANCELLED) {
                return WorkflowResult.failure("Workflow cancelled while paused");
            }

            WorkflowStep step = findStep(stepId);
            currentStepId = stepId;
            log.info("Executing step '{}' ({}) in workflow '{}'", step.name(), stepId, workflowId);

            try {
                // Build step request with input mapping
                AgentRequest stepRequest = buildStepRequest(step, request, contextData);

                // Execute via orchestrator
                AgentResponse response = orchestrator.orchestrate(stepRequest).join();

                // Store response
                stepResponses.put(stepId, response);
                Object stepOutput = response.output();
                stepResults.put(stepId, stepOutput != null ? stepOutput : response);

                // Apply output mapping to context
                applyOutputMapping(step, response, contextData);

                finalResponse = response;

                // Save checkpoint after step completion
                saveCheckpointAfterStep(stepId, contextData);

                log.info("Step '{}' completed in workflow '{}' with status: {}",
                    stepId, workflowId, response.status());

                // If step errored, fail the workflow
                if (response.isError()) {
                    log.error("Step '{}' failed in workflow '{}': {}", stepId, workflowId, response.output());
                    state.set(WorkflowState.FAILED);
                    return WorkflowResult.failure(
                        "Step '" + stepId + "' failed: " + response.output());
                }

            } catch (Exception e) {
                log.error("Step '{}' threw exception in workflow '{}'", stepId, workflowId, e);
                state.set(WorkflowState.FAILED);
                return WorkflowResult.failure(
                    "Step '" + stepId + "' threw exception: " + e.getMessage());
            }
        }

        state.set(WorkflowState.COMPLETED);
        completeCheckpoint();
        log.info("Workflow '{}' completed successfully", workflowId);
        return WorkflowResult.successWithSteps(finalResponse, new HashMap<>(stepResults));
    }

    /**
     * 从检查点恢复执行
     */
    private CompletableFuture<WorkflowResult> resumeFromCheckpoint(AgentRequest request) {
        state.set(WorkflowState.RUNNING);

        // Try to restore from checkpoint
        Map<String, Object> contextData = new HashMap<>(request.context());
        String resumeFromStep = currentStepId;

        if (stateManager != null) {
            Optional<Checkpoint> latestCheckpoint = stateManager.getLatestCheckpoint(workflowId);
            if (latestCheckpoint.isPresent()) {
                Checkpoint checkpoint = latestCheckpoint.get();
                resumeFromStep = checkpoint.nodeId();
                contextData.putAll(checkpoint.state());
                log.info("Restored workflow '{}' from checkpoint at step '{}'",
                    workflowId, resumeFromStep);
            }
        }

        // Restore step results from context
        restoreStepResults(contextData);

        List<String> sortedStepIds = topologicalSort();
        boolean shouldExecute = resumeFromStep == null;
        AgentResponse finalResponse = null;

        for (String stepId : sortedStepIds) {
            if (!shouldExecute) {
                if (stepId.equals(resumeFromStep)) {
                    shouldExecute = true;
                } else {
                    continue;
                }
            }

            // Check for cancellation
            if (state.get() == WorkflowState.CANCELLED) {
                return CompletableFuture.completedFuture(
                    WorkflowResult.failure("Workflow cancelled at step: " + stepId));
            }

            // Wait while paused
            awaitWhilePaused();
            if (state.get() == WorkflowState.CANCELLED) {
                return CompletableFuture.completedFuture(
                    WorkflowResult.failure("Workflow cancelled while paused"));
            }

            WorkflowStep step = findStep(stepId);
            currentStepId = stepId;
            log.info("Resuming step '{}' ({}) in workflow '{}'", step.name(), stepId, workflowId);

            try {
                AgentRequest stepRequest = buildStepRequest(step, request, contextData);
                AgentResponse response = orchestrator.orchestrate(stepRequest).join();

                stepResponses.put(stepId, response);
                Object stepOutput = response.output();
                stepResults.put(stepId, stepOutput != null ? stepOutput : response);

                applyOutputMapping(step, response, contextData);
                finalResponse = response;

                saveCheckpointAfterStep(stepId, contextData);

                if (response.isError()) {
                    state.set(WorkflowState.FAILED);
                    return CompletableFuture.completedFuture(
                        WorkflowResult.failure("Step '" + stepId + "' failed: " + response.output()));
                }

            } catch (Exception e) {
                state.set(WorkflowState.FAILED);
                return CompletableFuture.completedFuture(
                    WorkflowResult.failure("Step '" + stepId + "' threw exception: " + e.getMessage()));
            }
        }

        state.set(WorkflowState.COMPLETED);
        completeCheckpoint();
        return CompletableFuture.completedFuture(
            WorkflowResult.successWithSteps(finalResponse, new HashMap<>(stepResults)));
    }

    // ========== 拓扑排序 ==========

    /**
     * 对工作流步骤进行拓扑排序
     *
     * <p>使用 Kahn 算法，基于步骤的依赖关系确定执行顺序。
     * 如果存在循环依赖，抛出 IllegalStateException。
     *
     * @return 排序后的步骤 ID 列表
     */
    private List<String> topologicalSort() {
        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        // Initialize
        for (WorkflowStep step : steps) {
            adjacency.put(step.stepId(), new ArrayList<>());
            inDegree.put(step.stepId(), 0);
        }

        // Build graph
        for (WorkflowStep step : steps) {
            for (String dep : step.dependencies()) {
                if (!adjacency.containsKey(dep)) {
                    throw new IllegalStateException(
                        "Step '" + step.stepId() + "' depends on unknown step '" + dep + "'");
                }
                adjacency.get(dep).add(step.stepId());
                inDegree.merge(step.stepId(), 1, Integer::sum);
            }
        }

        // Kahn's algorithm
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            sorted.add(current);

            for (String neighbor : adjacency.get(current)) {
                int newDegree = inDegree.merge(neighbor, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (sorted.size() != steps.size()) {
            throw new IllegalStateException(
                "Circular dependency detected in workflow '" + workflowId + "'");
        }

        return sorted;
    }

    /**
     * 验证 DAG 无循环依赖
     */
    private void validateDag() {
        topologicalSort();
    }

    // ========== 步骤执行辅助 ==========

    private WorkflowStep findStep(String stepId) {
        return steps.stream()
            .filter(s -> s.stepId().equals(stepId))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Step '" + stepId + "' not found in workflow '" + workflowId + "'"));
    }

    /**
     * 构建步骤请求，应用输入映射
     */
    private AgentRequest buildStepRequest(
            WorkflowStep step,
            AgentRequest originalRequest,
            Map<String, Object> contextData
    ) {
        Map<String, Object> stepContext = new HashMap<>(contextData);

        // Apply input mapping: map context keys to step-specific keys
        if (step.inputMapping() != null) {
            for (Map.Entry<String, String> mapping : step.inputMapping().entrySet()) {
                Object value = contextData.get(mapping.getKey());
                if (value != null) {
                    stepContext.put(mapping.getValue(), value);
                }
            }
        }

        // Include previous step results
        stepContext.put("_stepResults", Map.copyOf(stepResults));

        return new AgentRequest(
            originalRequest.sessionId(),
            originalRequest.userInput(),
            stepContext,
            originalRequest.history()
        );
    }

    /**
     * 应用输出映射，将步骤输出写入上下文
     */
    private void applyOutputMapping(
            WorkflowStep step,
            AgentResponse response,
            Map<String, Object> contextData
    ) {
        if (step.outputMapping() != null && response.output() != null) {
            for (Map.Entry<String, String> mapping : step.outputMapping().entrySet()) {
                // Map from response key to context key
                contextData.put(mapping.getValue(), response.output());
            }
        }
        // Always store the full response output under the step ID
        contextData.put(step.stepId() + ".output", response.output());
    }

    /**
     * 等待暂停状态解除
     */
    private void awaitWhilePaused() {
        while (state.get() == WorkflowState.PAUSED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Workflow '{}' pause wait interrupted", workflowId);
                return;
            }
        }
    }

    // ========== 检查点管理 ==========

    private void saveCheckpointIfNeeded() {
        if (stateManager == null || checkpointPolicy == CheckpointPolicy.NONE) {
            return;
        }

        if (currentStepId != null) {
            Checkpoint checkpoint = Checkpoint.of(
                workflowId,
                currentStepId,
                Map.copyOf(stepResults)
            );
            stateManager.saveCheckpoint(workflowId, checkpoint);
            log.debug("Saved checkpoint for workflow '{}' at step '{}'", workflowId, currentStepId);
        }
    }

    private void saveCheckpointAfterStep(String stepId, Map<String, Object> contextData) {
        if (stateManager == null || checkpointPolicy == CheckpointPolicy.NONE) {
            return;
        }

        if (checkpointPolicy == CheckpointPolicy.EVERY_NODE
            || checkpointPolicy == CheckpointPolicy.EVERY_STAGE) {
            Checkpoint checkpoint = Checkpoint.of(
                workflowId,
                stepId,
                Map.copyOf(contextData)
            );
            stateManager.saveCheckpoint(workflowId, checkpoint);
            log.debug("Saved checkpoint for workflow '{}' after step '{}'", workflowId, stepId);
        }
    }

    private void completeCheckpoint() {
        if (stateManager != null) {
            stateManager.deleteState(workflowId);
            log.debug("Cleaned up state for completed workflow '{}'", workflowId);
        }
    }

    private void restoreStepResults(Map<String, Object> contextData) {
        // Restore step results from checkpoint context
        Object savedResults = contextData.get("_stepResults");
        if (savedResults instanceof Map<?, ?> resultsMap) {
            for (Map.Entry<?, ?> entry : resultsMap.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    stepResults.put(key, entry.getValue());
                }
            }
        }
    }
}

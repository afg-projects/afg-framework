package io.github.afgprojects.framework.ai.core.planning;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * Plan-Execute 执行器接口
 *
 * <p>PlanExecuteExecutor 实现 Plan-Execute 模式，提供：
 * <ul>
 *   <li>计划生成 - 根据目标生成执行计划</li>
 *   <li>计划执行 - 按步骤执行计划</li>
 *   <li>动态调整 - 根据执行情况调整计划</li>
 *   <li>错误处理 - 处理执行中的异常和失败</li>
 * </ul>
 *
 * <p>Plan-Execute 模式工作流程：
 * <pre>
 * 1. 接收目标 (Goal)
 * 2. 生成计划 (Plan): 将目标分解为可执行的步骤
 * 3. 执行计划 (Execute): 按顺序执行每个步骤
 * 4. 监控进度: 跟踪执行状态和结果
 * 5. 动态调整: 根据需要重新规划
 * 6. 返回最终结果
 * </pre>
 *
 * <p>实现示例：
 * <pre>{@code
 * public class DefaultPlanExecuteExecutor implements PlanExecuteExecutor {
 *     private final LlmClient llmClient;
 *     private final ToolRegistry toolRegistry;
 *
 *     @Override
 *     public PlanExecuteResult execute(String goal) {
 *         // 生成计划
 *         Plan plan = llmClient.generatePlan(goal);
 *
 *         // 执行计划
 *         List<Object> stepResults = new ArrayList<>();
 *         for (Step step : plan.getSteps()) {
 *             try {
 *                 Object result = toolRegistry.execute(step);
 *                 stepResults.add(result);
 *
 *                 // 检查是否需要重新规划
 *                 if (needsReplanning(result)) {
 *                     plan = llmClient.replan(goal, stepResults);
 *                 }
 *             } catch (Exception e) {
 *                 return PlanExecuteResult.failure(
 *                     "Step failed: " + step.getName(),
 *                     stepResults
 *                 );
 *             }
 *         }
 *
 *         return PlanExecuteResult.success(
 *             "Goal achieved: " + goal,
 *             stepResults
 *         );
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface PlanExecuteExecutor {

    /**
     * 执行 Plan-Execute 任务
     *
     * <p>根据目标生成计划并执行，返回最终结果。
     *
     * @param goal 目标描述
     * @return 执行结果
     */
    @NonNull
    PlanExecuteResult execute(@NonNull String goal);

    /**
     * 带计划验证的执行
     *
     * <p>在执行前验证计划的可行性，如果计划不可行则重新生成。
     *
     * @param goal 目标描述
     * @return 执行结果
     */
    @NonNull
    PlanExecuteResult executeWithValidation(@NonNull String goal);

    /**
     * 带动态调整的执行
     *
     * <p>在执行过程中根据实际情况动态调整计划。
     *
     * @param goal          目标描述
     * @param allowReplanning 是否允许重新规划
     * @return 执行结果
     */
    @NonNull
    PlanExecuteResult executeWithReplanning(@NonNull String goal, boolean allowReplanning);

    /**
     * 异步执行 Plan-Execute 任务
     *
     * <p>非阻塞执行，返回 CompletableFuture。
     *
     * @param goal 目标描述
     * @return 异步结果
     */
    @NonNull
    default CompletableFuture<PlanExecuteResult> executeAsync(@NonNull String goal) {
        return CompletableFuture.supplyAsync(() -> execute(goal));
    }

    /**
     * 带超时的执行
     *
     * <p>如果执行超时，返回失败结果。
     *
     * @param goal          目标描述
     * @param timeoutMillis 超时时间（毫秒）
     * @return 执行结果
     */
    @NonNull
    default PlanExecuteResult executeWithTimeout(@NonNull String goal, long timeoutMillis) {
        try {
            return executeAsync(goal)
                .get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return PlanExecuteResult.failure("Execution timed out after " + timeoutMillis + "ms", java.util.List.of());
        } catch (Exception e) {
            return PlanExecuteResult.failure("Execution failed: " + e.getMessage(), java.util.List.of());
        }
    }

    /**
     * 带进度回调的执行
     *
     * <p>每完成一个步骤后调用回调函数，用于实时监控执行进度。
     *
     * @param goal     目标描述
     * @param callback 进度回调函数
     * @return 执行结果
     */
    @NonNull
    default PlanExecuteResult executeWithProgress(
        @NonNull String goal,
        java.util.function.@NonNull Consumer<Object> callback
    ) {
        // 默认实现，子类可覆盖以提供更高效的实现
        return execute(goal);
    }
}

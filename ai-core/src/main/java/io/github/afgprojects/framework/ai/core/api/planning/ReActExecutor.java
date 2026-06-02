package io.github.afgprojects.framework.ai.core.api.planning;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * ReAct 执行器接口
 *
 * <p>ReActExecutor 实现 ReAct (Reasoning + Acting) 模式，提供：
 * <ul>
 *   <li>推理-行动循环 - 交替进行思考和执行</li>
 *   <li>工具调用 - 在行动阶段调用外部工具</li>
 *   <li>观察反馈 - 获取行动结果并继续推理</li>
 *   <li>终止条件 - 达到目标或超过最大步数时终止</li>
 * </ul>
 *
 * <p>ReAct 模式工作流程：
 * <pre>
 * 1. 接收任务 (Task)
 * 2. 推理 (Thought): 分析当前状态，决定下一步行动
 * 3. 行动 (Action): 执行工具调用或其他操作
 * 4. 观察 (Observation): 获取行动结果
 * 5. 重复 2-4 直到得出答案或达到限制
 * 6. 返回最终结果
 * </pre>
 *
 * <p>实现示例：
 * <pre>{@code
 * public class DefaultReActExecutor implements ReActExecutor {
 *     private final LlmClient llmClient;
 *     private final ToolRegistry toolRegistry;
 *     private final int maxSteps;
 *
 *     @Override
 *     public ReActResult execute(String task) {
 *         List<Object> steps = new ArrayList<>();
 *         String context = task;
 *
 *         for (int i = 0; i < maxSteps; i++) {
 *             // 推理阶段
 *             Thought thought = llmClient.generateThought(context);
 *             steps.add(thought);
 *
 *             // 检查是否得出答案
 *             if (thought.isFinalAnswer()) {
 *                 return ReActResult.success(thought.getAnswer(), steps);
 *             }
 *
 *             // 行动阶段
 *             Action action = thought.getAction();
 *             Observation observation = toolRegistry.execute(action);
 *             steps.add(action);
 *             steps.add(observation);
 *
 *             // 更新上下文
 *             context = context + "\n" + thought + "\n" + action + "\n" + observation;
 *         }
 *
 *         return ReActResult.failure("Exceeded maximum steps", steps);
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ReActExecutor {

    /**
     * 执行 ReAct 任务
     *
     * <p>同步执行 ReAct 循环，直到得出答案或达到限制。
     *
     * @param task 任务描述
     * @return 执行结果
     */
    @NonNull
    ReActResult execute(@NonNull String task);

    /**
     * 带最大步数限制的执行
     *
     * <p>限制推理-行动循环的最大次数，防止无限循环。
     *
     * @param task     任务描述
     * @param maxSteps 最大步数
     * @return 执行结果
     */
    @NonNull
    ReActResult executeWithMaxSteps(@NonNull String task, int maxSteps);

    /**
     * 异步执行 ReAct 任务
     *
     * <p>非阻塞执行，返回 CompletableFuture。
     *
     * @param task 任务描述
     * @return 异步结果
     */
    @NonNull
    default CompletableFuture<ReActResult> executeAsync(@NonNull String task) {
        return CompletableFuture.supplyAsync(() -> execute(task));
    }

    /**
     * 带超时的执行
     *
     * <p>如果执行超时，返回失败结果。
     *
     * @param task          任务描述
     * @param timeoutMillis 超时时间（毫秒）
     * @return 执行结果
     */
    @NonNull
    default ReActResult executeWithTimeout(@NonNull String task, long timeoutMillis) {
        try {
            return executeAsync(task)
                .get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return ReActResult.failure("Execution timed out after " + timeoutMillis + "ms", java.util.List.of());
        } catch (Exception e) {
            return ReActResult.failure("Execution failed: " + e.getMessage(), java.util.List.of());
        }
    }

    /**
     * 带回调的执行
     *
     * <p>每完成一个步骤后调用回调函数，用于实时监控执行过程。
     *
     * @param task     任务描述
     * @param callback 步骤回调函数
     * @return 执行结果
     */
    @NonNull
    default ReActResult executeWithCallback(
        @NonNull String task,
        java.util.function.@NonNull Consumer<Object> callback
    ) {
        // 默认实现，子类可覆盖以提供更高效的实现
        return execute(task);
    }
}

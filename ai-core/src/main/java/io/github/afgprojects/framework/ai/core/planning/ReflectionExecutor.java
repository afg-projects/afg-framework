package io.github.afgprojects.framework.ai.core.planning;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.CompletableFuture;

/**
 * Reflection 执行器接口
 *
 * <p>ReflectionExecutor 实现 Reflection (反思) 模式，提供：
 * <ul>
 *   <li>响应生成 - 生成初始响应</li>
 *   <li>自我反思 - 对响应进行反思和评估</li>
 *   <li>迭代改进 - 根据反思改进响应</li>
 *   <li>质量验证 - 验证最终响应的质量</li>
 * </ul>
 *
 * <p>Reflection 模式工作流程：
 * <pre>
 * 1. 接收任务 (Task)
 * 2. 生成初始响应 (Response)
 * 3. 反思响应 (Reflection):
 *    - 正确性检查
 *    - 完整性检查
 *    - 改进建议
 * 4. 可选：迭代改进
 * 5. 返回最终结果
 * </pre>
 *
 * <p>实现示例：
 * <pre>{@code
 * public class DefaultReflectionExecutor implements ReflectionExecutor {
 *     private final LlmClient llmClient;
 *     private final int maxIterations;
 *
 *     @Override
 *     public ReflectionResult execute(String task) {
 *         // 生成初始响应
 *         Object response = llmClient.generate(task);
 *
 *         // 反思响应
 *         String reflection = llmClient.reflect(task, response);
 *
 *         // 检查是否需要改进
 *         if (needsImprovement(reflection)) {
 *             response = llmClient.improve(task, response, reflection);
 *             reflection = llmClient.reflect(task, response);
 *         }
 *
 *         return ReflectionResult.success(response, reflection);
 *     }
 *
 *     @Override
 *     public ReflectionResult executeWithIterations(String task, int iterations) {
 *         Object response = llmClient.generate(task);
 *         String reflection = null;
 *
 *         for (int i = 0; i < iterations; i++) {
 *             reflection = llmClient.reflect(task, response);
 *             if (!needsImprovement(reflection)) {
 *                 break;
 *             }
 *             response = llmClient.improve(task, response, reflection);
 *         }
 *
 *         return ReflectionResult.success(response, reflection);
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ReflectionExecutor {

    /**
     * 执行 Reflection 任务
     *
     * <p>生成响应并进行反思，返回结果。
     *
     * @param task 任务描述
     * @return 执行结果
     */
    @NonNull
    ReflectionResult execute(@NonNull String task);

    /**
     * 带迭代次数的执行
     *
     * <p>进行多轮反思和改进，直到达到指定迭代次数或响应质量达标。
     *
     * @param task       任务描述
     * @param iterations 迭代次数
     * @return 执行结果
     */
    @NonNull
    ReflectionResult executeWithIterations(@NonNull String task, int iterations);

    /**
     * 带质量阈值的执行
     *
     * <p>持续迭代直到响应质量达到阈值或超过最大迭代次数。
     *
     * @param task          任务描述
     * @param qualityThreshold 质量阈值 (0.0 - 1.0)
     * @param maxIterations   最大迭代次数
     * @return 执行结果
     */
    @NonNull
    ReflectionResult executeWithQualityThreshold(
        @NonNull String task,
        double qualityThreshold,
        int maxIterations
    );

    /**
     * 异步执行 Reflection 任务
     *
     * <p>非阻塞执行，返回 CompletableFuture。
     *
     * @param task 任务描述
     * @return 异步结果
     */
    @NonNull
    default CompletableFuture<ReflectionResult> executeAsync(@NonNull String task) {
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
    default ReflectionResult executeWithTimeout(@NonNull String task, long timeoutMillis) {
        try {
            return executeAsync(task)
                .get(timeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return ReflectionResult.failure(null, "Execution timed out after " + timeoutMillis + "ms");
        } catch (Exception e) {
            return ReflectionResult.failure(null, "Execution failed: " + e.getMessage());
        }
    }

    /**
     * 带回调的执行
     *
     * <p>每完成一轮反思后调用回调函数，用于实时监控反思过程。
     *
     * @param task     任务描述
     * @param callback 反思回调函数
     * @return 执行结果
     */
    @NonNull
    default ReflectionResult executeWithCallback(
        @NonNull String task,
        java.util.function.@NonNull BiConsumer<Object, String> callback
    ) {
        // 默认实现，子类可覆盖以提供更高效的实现
        return execute(task);
    }
}

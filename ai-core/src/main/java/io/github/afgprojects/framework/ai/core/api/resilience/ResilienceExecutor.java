package io.github.afgprojects.framework.ai.core.api.resilience;

import org.jspecify.annotations.NonNull;

/**
 * 韧性执行器接口
 *
 * <p>组合重试、熔断、降级策略，提供完整的故障保护：
 * <pre>
 * 请求 → 熔断器检查 → 执行 → 成功/失败 → 重试/降级
 * </pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ResilienceExecutor {

    /**
     * 执行带韧性保护的操作
     *
     * @param operation      要执行的操作
     * @param <T>            返回类型
     * @return 操作结果
     * @throws Exception 如果所有策略都失败
     */
    @NonNull
    <T> T execute(@NonNull ResilientOperation<T> operation) throws Exception;

    /**
     * 执行带韧性保护的操作（带降级）
     *
     * @param operation      要执行的操作
     * @param fallback       降级策略
     * @param <T>            返回类型
     * @return 操作结果
     */
    @NonNull
    <T> T executeWithFallback(
            @NonNull ResilientOperation<T> operation,
            @NonNull FallbackStrategy<T> fallback
    );

    /**
     * 获取重试策略
     *
     * @return 重试策略
     */
    @NonNull
    RetryPolicy getRetryPolicy();

    /**
     * 获取熔断器
     *
     * @return 熔断器
     */
    @NonNull
    CircuitBreaker getCircuitBreaker();

    /**
     * 可韧性保护的操作
     *
     * @param <T> 返回类型
     */
    @FunctionalInterface
    interface ResilientOperation<T> {
        T execute() throws Exception;
    }
}
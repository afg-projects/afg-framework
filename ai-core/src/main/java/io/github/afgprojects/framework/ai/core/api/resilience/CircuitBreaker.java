package io.github.afgprojects.framework.ai.core.api.resilience;

import org.jspecify.annotations.NonNull;

/**
 * 熔断器接口
 *
 * <p>防止故障级联扩散，当错误率达到阈值时自动熔断：
 * <ul>
 *   <li>CLOSED - 正常状态，允许请求通过</li>
 *   <li>OPEN - 熔断状态，拒绝所有请求</li>
 *   <li>HALF_OPEN - 半开状态，允许少量请求探测恢复</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface CircuitBreaker {

    /**
     * 熔断器状态
     */
    enum State {
        /**
         * 正常状态，允许请求通过
         */
        CLOSED,
        /**
         * 熔断状态，拒绝所有请求
         */
        OPEN,
        /**
         * 半开状态，允许探测请求
         */
        HALF_OPEN
    }

    /**
     * 获取当前状态
     *
     * @return 熔断器状态
     */
    @NonNull
    State getState();

    /**
     * 判断是否允许请求通过
     *
     * @return 是否允许
     */
    boolean allowRequest();

    /**
     * 记录成功请求
     */
    void recordSuccess();

    /**
     * 记录失败请求
     *
     * @param exception 导致失败的异常
     */
    void recordFailure(@NonNull Exception exception);

    /**
     * 执行带熔断保护的操作
     *
     * @param operation 要执行的操作
     * @param <T>       返回类型
     * @return 操作结果
     * @throws CircuitBreakerException 如果熔断器处于 OPEN 状态
     * @throws Exception 如果操作执行失败
     */
    @NonNull
    <T> T execute(@NonNull CircuitBreakerOperation<T> operation) throws Exception;

    /**
     * 强制打开熔断器
     */
    void forceOpen();

    /**
     * 强制关闭熔断器
     */
    void forceClose();

    /**
     * 获取统计信息
     *
     * @return 统计信息
     */
    @NonNull
    CircuitBreakerStats getStats();

    /**
     * 可熔断保护的操作
     *
     * @param <T> 返回类型
     */
    @FunctionalInterface
    interface CircuitBreakerOperation<T> {
        T execute() throws Exception;
    }

    /**
     * 熔断器统计信息
     */
    interface CircuitBreakerStats {
        /**
         * 获取成功请求数
         */
        long getSuccessCount();

        /**
         * 获取失败请求数
         */
        long getFailureCount();

        /**
         * 获取失败率
         */
        double getFailureRate();

        /**
         * 获取总请求数
         */
        long getTotalCount();
    }
}
package io.github.afgprojects.framework.ai.core.resilience;

import org.jspecify.annotations.NonNull;

/**
 * 熔断器异常
 *
 * <p>当熔断器处于 OPEN 状态拒绝请求时抛出。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class CircuitBreakerException extends RuntimeException {

    private static final String ERROR_CODE = "CIRCUIT_BREAKER_OPEN";

    private final CircuitBreaker.State state;

    /**
     * 创建熔断器异常
     *
     * @param state 熔断器状态
     */
    public CircuitBreakerException(CircuitBreaker.State state) {
        super("Circuit breaker is " + state + ", requests are rejected");
        this.state = state;
    }

    /**
     * 创建熔断器异常（带消息）
     *
     * @param state   熔断器状态
     * @param message 详细消息
     */
    public CircuitBreakerException(CircuitBreaker.State state, String message) {
        super(message);
        this.state = state;
    }

    /**
     * 获取熔断器状态
     *
     * @return 状态
     */
    public CircuitBreaker.State getState() {
        return state;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    @NonNull
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
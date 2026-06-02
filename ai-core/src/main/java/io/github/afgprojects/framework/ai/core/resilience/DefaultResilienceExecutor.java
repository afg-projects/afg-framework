package io.github.afgprojects.framework.ai.core.resilience;

import io.github.afgprojects.framework.ai.core.api.resilience.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * 默认韧性执行器实现
 *
 * <p>组合重试、熔断、降级策略：
 * <pre>
 * 请求 → 熔断器检查 → 执行 → 成功/失败 → 重试 → 降级
 * </pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultResilienceExecutor implements ResilienceExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultResilienceExecutor.class);

    private final RetryPolicy retryPolicy;
    private final CircuitBreaker circuitBreaker;

    /**
     * 创建韧性执行器
     *
     * @param retryPolicy   重试策略
     * @param circuitBreaker 熔断器
     */
    public DefaultResilienceExecutor(
            @NonNull RetryPolicy retryPolicy,
            @NonNull CircuitBreaker circuitBreaker
    ) {
        this.retryPolicy = retryPolicy;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * 创建默认韧性执行器
     */
    public DefaultResilienceExecutor() {
        this(new DefaultRetryPolicy(), new DefaultCircuitBreaker());
    }

    @Override
    @NonNull
    public <T> T execute(@NonNull ResilientOperation<T> operation) throws Exception {
        return executeWithFallback(operation, new ThrowingFallbackStrategy<>());
    }

    @Override
    @NonNull
    public <T> T executeWithFallback(
            @NonNull ResilientOperation<T> operation,
            @NonNull FallbackStrategy<T> fallback
    ) {
        String requestId = generateRequestId();

        try {
            // 熔断器检查
            if (!circuitBreaker.allowRequest()) {
                log.warn("Request {} rejected by circuit breaker, state: {}",
                        requestId, circuitBreaker.getState());
                return handleFallback(new CircuitBreakerException(circuitBreaker.getState()),
                        fallback, requestId);
            }

            // 执行操作（带重试）
            T result = executeWithRetry(operation, requestId);

            // 记录成功
            circuitBreaker.recordSuccess();

            return result;

        } catch (Exception e) {
            // 记录失败
            circuitBreaker.recordFailure(e);

            // 尝试降级
            return handleFallback(e, fallback, requestId);
        }
    }

    /**
     * 执行带重试的操作
     */
    private <T> T executeWithRetry(ResilientOperation<T> operation, String requestId) throws Exception {
        return retryPolicy.execute(() -> {
            log.debug("Executing operation for request {}", requestId);
            return operation.execute();
        });
    }

    /**
     * 处理降级
     */
    private <T> T handleFallback(
            Exception exception,
            FallbackStrategy<T> fallback,
            String requestId
    ) {
        if (fallback.shouldFallback(exception)) {
            log.info("Executing fallback for request {} due to: {}", requestId, exception.getMessage());

            FallbackStrategy.FallbackContext context = new DefaultFallbackContext(requestId);
            T result = fallback.fallback(exception, context);

            if (result != null) {
                return result;
            }
        }

        // 如果降级失败或不应降级，抛出原始异常
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        throw new RuntimeException("Operation failed and fallback unsuccessful", exception);
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    @NonNull
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    @Override
    @NonNull
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * 默认降级上下文
     */
    private static class DefaultFallbackContext implements FallbackStrategy.FallbackContext {
        private final String requestId;

        DefaultFallbackContext(String requestId) {
            this.requestId = requestId;
        }

        @Override
        @Nullable
        public Object getOriginalRequest() {
            return null;
        }

        @Override
        @NonNull
        public String getRequestId() {
            return requestId;
        }

        @Override
        public long getRequestTimestamp() {
            return System.currentTimeMillis();
        }
    }

    /**
     * 抛出异常的降级策略
     */
    private static class ThrowingFallbackStrategy<T> implements FallbackStrategy<T> {
        @Override
        @Nullable
        public T fallback(@NonNull Exception exception, @NonNull FallbackContext context) {
            return null;
        }

        @Override
        public boolean shouldFallback(@NonNull Exception exception) {
            return false;
        }
    }

    /**
     * 创建 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder
     */
    public static class Builder {
        private RetryPolicy retryPolicy = new DefaultRetryPolicy();
        private CircuitBreaker circuitBreaker = new DefaultCircuitBreaker();

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder circuitBreaker(CircuitBreaker circuitBreaker) {
            this.circuitBreaker = circuitBreaker;
            return this;
        }

        public DefaultResilienceExecutor build() {
            return new DefaultResilienceExecutor(retryPolicy, circuitBreaker);
        }
    }
}

package io.github.afgprojects.framework.ai.core.resilience;

import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreaker;
import io.github.afgprojects.framework.ai.core.api.resilience.CircuitBreakerException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认熔断器实现
 *
 * <p>基于滑动窗口的熔断器：
 * <ul>
 *   <li>使用滑动窗口统计成功率</li>
 *   <li>支持配置失败率阈值</li>
 *   <li>支持配置半开状态的探测请求数</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultCircuitBreaker implements CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(DefaultCircuitBreaker.class);

    private final String name;
    private final int windowSize;
    private final double failureRateThreshold;
    private final int halfOpenMaxCalls;
    private final long openStateTimeoutMs;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private final AtomicInteger halfOpenCalls = new AtomicInteger(0);

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 创建熔断器
     *
     * @param name                熔断器名称
     * @param windowSize          滑动窗口大小
     * @param failureRateThreshold 失败率阈值（0-1）
     * @param halfOpenMaxCalls    半开状态最大探测次数
     * @param openStateTimeoutMs  开启状态超时时间（毫秒）
     */
    public DefaultCircuitBreaker(
            String name,
            int windowSize,
            double failureRateThreshold,
            int halfOpenMaxCalls,
            long openStateTimeoutMs
    ) {
        this.name = name;
        this.windowSize = windowSize;
        this.failureRateThreshold = failureRateThreshold;
        this.halfOpenMaxCalls = halfOpenMaxCalls;
        this.openStateTimeoutMs = openStateTimeoutMs;
    }

    /**
     * 创建默认熔断器
     */
    public DefaultCircuitBreaker() {
        this("default", 100, 0.5, 10, 30000);
    }

    @Override
    @NonNull
    public State getState() {
        return state.get();
    }

    @Override
    public boolean allowRequest() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                // 检查是否应该尝试进入半开状态
                long timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get();
                if (timeSinceLastFailure >= openStateTimeoutMs) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        halfOpenCalls.set(0);
                        log.info("Circuit breaker '{}' transitioned from OPEN to HALF_OPEN", name);
                        return true;
                    }
                }
                return false;

            case HALF_OPEN:
                // 半开状态限制请求数量
                return halfOpenCalls.incrementAndGet() <= halfOpenMaxCalls;

            default:
                return false;
        }
    }

    @Override
    public void recordSuccess() {
        successCount.incrementAndGet();

        State currentState = state.get();
        if (currentState == State.HALF_OPEN) {
            // 半开状态成功，尝试关闭熔断器
            if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                resetCounters();
                log.info("Circuit breaker '{}' transitioned from HALF_OPEN to CLOSED", name);
            }
        }

        checkThreshold();
    }

    @Override
    public void recordFailure(@NonNull Exception exception) {
        failureCount.incrementAndGet();
        lastFailureTime.set(System.currentTimeMillis());

        State currentState = state.get();
        if (currentState == State.HALF_OPEN) {
            // 半开状态失败，立即打开熔断器
            if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                log.warn("Circuit breaker '{}' transitioned from HALF_OPEN to OPEN due to failure: {}",
                        name, exception.getMessage());
            }
        } else if (currentState == State.CLOSED) {
            checkThreshold();
        }
    }

    @Override
    @NonNull
    public <T> T execute(@NonNull CircuitBreakerOperation<T> operation) throws Exception {
        if (!allowRequest()) {
            throw new CircuitBreakerException(state.get(),
                    "Circuit breaker '" + name + "' is " + state.get());
        }

        try {
            T result = operation.execute();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure(e);
            throw e;
        }
    }

    @Override
    public void forceOpen() {
        state.set(State.OPEN);
        lastFailureTime.set(System.currentTimeMillis());
        log.warn("Circuit breaker '{}' forced to OPEN", name);
    }

    @Override
    public void forceClose() {
        state.set(State.CLOSED);
        resetCounters();
        log.info("Circuit breaker '{}' forced to CLOSED", name);
    }

    @Override
    @NonNull
    public CircuitBreakerStats getStats() {
        return new DefaultCircuitBreakerStats(
                successCount.get(),
                failureCount.get()
        );
    }

    private void checkThreshold() {
        int total = successCount.get() + failureCount.get();
        if (total >= windowSize) {
            double failureRate = (double) failureCount.get() / total;
            if (failureRate >= failureRateThreshold) {
                if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                    lastFailureTime.set(System.currentTimeMillis());
                    log.warn("Circuit breaker '{}' transitioned from CLOSED to OPEN, failure rate: {}%",
                            name, String.format("%.2f", failureRate * 100));
                }
            }
        }
    }

    private void resetCounters() {
        successCount.set(0);
        failureCount.set(0);
    }

    /**
     * 默认统计信息实现
     */
    private static class DefaultCircuitBreakerStats implements CircuitBreakerStats {
        private final long successCount;
        private final long failureCount;

        DefaultCircuitBreakerStats(long successCount, long failureCount) {
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        @Override
        public long getSuccessCount() {
            return successCount;
        }

        @Override
        public long getFailureCount() {
            return failureCount;
        }

        @Override
        public double getFailureRate() {
            long total = successCount + failureCount;
            return total > 0 ? (double) failureCount / total : 0.0;
        }

        @Override
        public long getTotalCount() {
            return successCount + failureCount;
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
        private String name = "default";
        private int windowSize = 100;
        private double failureRateThreshold = 0.5;
        private int halfOpenMaxCalls = 10;
        private long openStateTimeoutMs = 30000;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        public Builder failureRateThreshold(double failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        public Builder halfOpenMaxCalls(int halfOpenMaxCalls) {
            this.halfOpenMaxCalls = halfOpenMaxCalls;
            return this;
        }

        public Builder openStateTimeoutMs(long openStateTimeoutMs) {
            this.openStateTimeoutMs = openStateTimeoutMs;
            return this;
        }

        public DefaultCircuitBreaker build() {
            return new DefaultCircuitBreaker(name, windowSize, failureRateThreshold, halfOpenMaxCalls, openStateTimeoutMs);
        }
    }
}

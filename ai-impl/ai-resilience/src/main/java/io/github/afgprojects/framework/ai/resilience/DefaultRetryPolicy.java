package io.github.afgprojects.framework.ai.resilience;

import io.github.afgprojects.framework.ai.core.api.resilience.RetryPolicy;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 默认重试策略实现
 *
 * <p>支持：
 * <ul>
 *   <li>指数退避</li>
 *   <li>最大重试次数</li>
 *   <li>可配置的可重试异常类型</li>
 *   <li>抖动避免惊群效应</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultRetryPolicy implements RetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(DefaultRetryPolicy.class);

    private final int maxRetries;
    private final long initialIntervalMs;
    private final double multiplier;
    private final long maxIntervalMs;
    private final double jitterFactor;
    private final Set<Class<? extends Exception>> retryableExceptions;

    /**
     * 创建默认重试策略
     *
     * @param maxRetries         最大重试次数
     * @param initialIntervalMs  初始重试间隔（毫秒）
     * @param multiplier         退避乘数
     * @param maxIntervalMs      最大重试间隔（毫秒）
     * @param jitterFactor       抖动因子（0-1）
     */
    public DefaultRetryPolicy(
            int maxRetries,
            long initialIntervalMs,
            double multiplier,
            long maxIntervalMs,
            double jitterFactor
    ) {
        this.maxRetries = maxRetries;
        this.initialIntervalMs = initialIntervalMs;
        this.multiplier = multiplier;
        this.maxIntervalMs = maxIntervalMs;
        this.jitterFactor = jitterFactor;
        this.retryableExceptions = new HashSet<>();

        // 默认可重试的异常类型
        retryableExceptions.add(java.net.SocketTimeoutException.class);
        retryableExceptions.add(java.net.ConnectException.class);
        retryableExceptions.add(java.io.IOException.class);
    }

    /**
     * 创建默认重试策略（使用默认参数）
     */
    public DefaultRetryPolicy() {
        this(3, 1000, 2.0, 30000, 0.5);
    }

    /**
     * 添加可重试的异常类型
     *
     * @param exceptionClass 异常类型
     */
    public void addRetryableException(Class<? extends Exception> exceptionClass) {
        retryableExceptions.add(exceptionClass);
    }

    @Override
    public boolean shouldRetry(@NonNull Exception exception, int retryCount) {
        if (retryCount >= maxRetries) {
            log.debug("Max retries ({}) reached, not retrying", maxRetries);
            return false;
        }

        // 检查异常类型
        for (Class<? extends Exception> retryableType : retryableExceptions) {
            if (retryableType.isInstance(exception)) {
                log.debug("Exception {} is retryable, attempt {}/{}",
                        exception.getClass().getSimpleName(), retryCount + 1, maxRetries);
                return true;
            }
        }

        // 检查异常消息中的可重试关键字
        String message = exception.getMessage();
        if (message != null && (
                message.contains("timeout") ||
                message.contains("rate limit") ||
                message.contains("overloaded") ||
                message.contains("503") ||
                message.contains("429"))) {
            log.debug("Exception message indicates retryable condition: {}", message);
            return true;
        }

        log.debug("Exception {} is not retryable", exception.getClass().getSimpleName());
        return false;
    }

    @Override
    public long getWaitTime(int retryCount) {
        // 计算指数退避时间
        long waitTime = (long) (initialIntervalMs * Math.pow(multiplier, retryCount));
        waitTime = Math.min(waitTime, maxIntervalMs);

        // 添加抖动
        if (jitterFactor > 0) {
            double jitter = waitTime * jitterFactor * ThreadLocalRandom.current().nextDouble();
            waitTime = (long) (waitTime + jitter - (waitTime * jitterFactor / 2));
        }

        return Math.max(0, waitTime);
    }

    @Override
    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    @NonNull
    public <T> T execute(@NonNull RetryableOperation<T> operation) throws Exception {
        Exception lastException = null;
        int retryCount = 0;

        while (true) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;

                if (!shouldRetry(e, retryCount)) {
                    throw e;
                }

                long waitTime = getWaitTime(retryCount);
                log.info("Retry attempt {}/{} after {}ms due to: {}",
                        retryCount + 1, maxRetries, waitTime, e.getMessage());

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    e.addSuppressed(ie);
                    throw e;
                }

                retryCount++;
            }
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
        private int maxRetries = 3;
        private long initialIntervalMs = 1000;
        private double multiplier = 2.0;
        private long maxIntervalMs = 30000;
        private double jitterFactor = 0.5;

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialIntervalMs(long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxIntervalMs(long maxIntervalMs) {
            this.maxIntervalMs = maxIntervalMs;
            return this;
        }

        public Builder jitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
            return this;
        }

        public DefaultRetryPolicy build() {
            return new DefaultRetryPolicy(maxRetries, initialIntervalMs, multiplier, maxIntervalMs, jitterFactor);
        }
    }
}
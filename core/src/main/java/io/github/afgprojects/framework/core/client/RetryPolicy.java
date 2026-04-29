package io.github.afgprojects.framework.core.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * 重试策略
 */
public interface RetryPolicy {

    /**
     * 获取最大重试次数
     */
    int getMaxAttempts();

    /**
     * 计算第 n 次重试的等待时间
     *
     * @param attempt 当前尝试次数（从 1 开始）
     * @return 等待时间
     */
    Duration getWaitDuration(int attempt);

    /**
     * 判断是否应该重试
     *
     * @param statusCode HTTP 状态码
     * @param exception  异常（如果有）
     * @return 如果应该重试返回 true
     */
    boolean shouldRetry(int statusCode, @Nullable Throwable exception);

    /**
     * 创建默认重试策略
     */
    static RetryPolicy defaultPolicy() {
        return builder().build();
    }

    /**
     * 创建重试策略构建器
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * 重试策略构建器
     */
    class Builder {
        private int maxAttempts = 3;
        private long initialInterval = 1000;
        private double multiplier = 2.0;
        private long maxInterval = 10000;
        private Set<Integer> retryOnStatus = Set.of(502, 503, 504);
        /**
         * 默认只对网络相关异常进行重试，避免对不可恢复错误进行无效重试。
         * <p>
         * 可通过网络异常包括：IOException 及其子类（如 SocketTimeoutException、ConnectException）。
         * 业务异常、参数校验异常等不进行重试。
         * <p>
         * 同时检查异常链中的 cause，支持 RuntimeException 包装的 IOException。
         */
        private Predicate<Throwable> retryOnException = e -> {
            // 检查顶层异常
            if (e instanceof IOException || e instanceof SocketTimeoutException || e instanceof ConnectException) {
                return true;
            }
            // 检查 cause 链
            Throwable cause = e.getCause();
            while (cause != null) {
                if (cause instanceof IOException || cause instanceof SocketTimeoutException
                        || cause instanceof ConnectException) {
                    return true;
                }
                cause = cause.getCause();
            }
            return false;
        };

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder initialInterval(long initialInterval) {
            this.initialInterval = initialInterval;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder maxInterval(long maxInterval) {
            this.maxInterval = maxInterval;
            return this;
        }

        public Builder retryOnStatus(Set<Integer> statusCodes) {
            this.retryOnStatus = statusCodes;
            return this;
        }

        public Builder retryOnException(Predicate<Throwable> predicate) {
            this.retryOnException = predicate;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy() {
                @Override
                public int getMaxAttempts() {
                    return maxAttempts;
                }

                @Override
                public Duration getWaitDuration(int attempt) {
                    long baseInterval = (long) (initialInterval * Math.pow(multiplier, attempt - 1));
                    long cappedInterval = Math.min(baseInterval, maxInterval);
                    // 添加 10-25% 随机抖动，防止重试风暴
                    long jitter = (long) (cappedInterval * 0.1 * ThreadLocalRandom.current().nextDouble(1, 2.5));
                    return Duration.ofMillis(cappedInterval + jitter);
                }

                @Override
                public boolean shouldRetry(int statusCode, @Nullable Throwable exception) {
                    return retryOnStatus.contains(statusCode)
                            || (exception != null && retryOnException.test(exception));
                }
            };
        }
    }
}

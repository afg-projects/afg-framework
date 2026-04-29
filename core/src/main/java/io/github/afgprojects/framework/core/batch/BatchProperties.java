package io.github.afgprojects.framework.core.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 批量操作配置属性
 * <p>
 * 配置批量操作的默认行为，如批次大小、并行度、错误容忍率等
 * </p>
 */
@ConfigurationProperties(prefix = "afg.batch")
public class BatchProperties {

    /**
     * 默认批次大小
     * <p>
     * 每批次处理的元素数量
     * </p>
     */
    private int defaultBatchSize = 100;

    /**
     * 默认并行度
     * <p>
     * 并行执行时的线程池大小，0 表示使用 CPU 核心数
     * </p>
     */
    private int defaultParallelism = 0;

    /**
     * 错误容忍率
     * <p>
     * 当失败率超过此阈值时终止处理，取值范围 0.0 - 1.0
     * 值为 1.0 表示不限制错误容忍
     * </p>
     */
    private double errorTolerance = 1.0;

    /**
     * 是否在遇到错误时立即停止
     * <p>
     * 为 true 时遇到第一个错误就停止处理
     * 为 false 时继续处理剩余元素
     * </p>
     */
    private boolean stopOnError = false;

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 限流配置
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    public int getDefaultBatchSize() {
        return defaultBatchSize;
    }

    public void setDefaultBatchSize(int defaultBatchSize) {
        this.defaultBatchSize = defaultBatchSize;
    }

    public int getDefaultParallelism() {
        return defaultParallelism;
    }

    public void setDefaultParallelism(int defaultParallelism) {
        this.defaultParallelism = defaultParallelism;
    }

    public double getErrorTolerance() {
        return errorTolerance;
    }

    public void setErrorTolerance(double errorTolerance) {
        this.errorTolerance = errorTolerance;
    }

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public RateLimitConfig getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimitConfig rateLimit) {
        this.rateLimit = rateLimit;
    }

    /**
     * 获取实际并行度
     * <p>
     * 如果配置为 0，返回 CPU 核心数
     * </p>
     *
     * @return 实际并行度
     */
    public int getActualParallelism() {
        return defaultParallelism > 0 ? defaultParallelism : Runtime.getRuntime().availableProcessors();
    }

    /**
     * 重试配置
     */
    public static class RetryConfig {
        /**
         * 是否启用重试
         */
        private boolean enabled = false;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 初始重试间隔（毫秒）
         */
        private long initialInterval = 1000;

        /**
         * 重试间隔乘数（指数退避）
         */
        private double multiplier = 2.0;

        /**
         * 最大重试间隔（毫秒）
         */
        private long maxInterval = 10000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialInterval() {
            return initialInterval;
        }

        public void setInitialInterval(long initialInterval) {
            this.initialInterval = initialInterval;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxInterval() {
            return maxInterval;
        }

        public void setMaxInterval(long maxInterval) {
            this.maxInterval = maxInterval;
        }
    }

    /**
     * 限流配置
     */
    public static class RateLimitConfig {
        /**
         * 是否启用限流
         */
        private boolean enabled = false;

        /**
         * 每秒最大请求数
         */
        private int permitsPerSecond = 100;

        /**
         * 等待许可的最大时间（毫秒），超过则抛出异常
         * 值为 0 表示不等待，直接拒绝
         * 值为负数表示无限等待
         */
        private long maxWaitMillis = 5000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public void setPermitsPerSecond(int permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
        }

        public long getMaxWaitMillis() {
            return maxWaitMillis;
        }

        public void setMaxWaitMillis(long maxWaitMillis) {
            this.maxWaitMillis = maxWaitMillis;
        }
    }
}

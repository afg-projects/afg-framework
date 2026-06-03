package io.github.afgprojects.framework.core.properties.batch;

import lombok.Data;

/**
 * 批量操作配置。
 */
@Data
public class AfgCoreBatchProperties {

    /**
     * 默认批次大小。
     */
    private int defaultBatchSize = 100;

    /**
     * 默认并行度。
     */
    private int defaultParallelism = 0;

    /**
     * 错误容忍率。
     */
    private double errorTolerance = 1.0;

    /**
     * 是否在遇到错误时立即停止。
     */
    private boolean stopOnError = false;

    /**
     * 重试配置。
     */
    private AfgCoreBatchRetryProperties retry = new AfgCoreBatchRetryProperties();

    /**
     * 限流配置。
     */
    private AfgCoreBatchRateLimitProperties rateLimit = new AfgCoreBatchRateLimitProperties();

    /**
     * 获取实际并行度。
     */
    public int getActualParallelism() {
        return defaultParallelism > 0 ? defaultParallelism : Runtime.getRuntime().availableProcessors();
    }
}

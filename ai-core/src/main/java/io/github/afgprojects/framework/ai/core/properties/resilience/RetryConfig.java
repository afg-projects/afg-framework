package io.github.afgprojects.framework.ai.core.properties.resilience;

import lombok.Data;

/**
 * 重试配置。
 */
@Data
public class RetryConfig {

    /**
     * 最大重试次数。
     */
    private int maxRetries = 3;

    /**
     * 初始重试间隔（毫秒）。
     */
    private long initialIntervalMs = 1000;

    /**
     * 退避乘数。
     */
    private double multiplier = 2.0;

    /**
     * 最大重试间隔（毫秒）。
     */
    private long maxIntervalMs = 30000;

    /**
     * 抖动因子（0.0 ~ 1.0）。
     */
    private double jitterFactor = 0.5;
}

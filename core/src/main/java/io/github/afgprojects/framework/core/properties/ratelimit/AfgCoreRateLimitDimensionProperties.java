package io.github.afgprojects.framework.core.properties.ratelimit;

import org.jspecify.annotations.Nullable;

import lombok.Data;

/**
 * 限流维度配置。
 */
@Data
public class AfgCoreRateLimitDimensionProperties {

    /**
     * 每秒请求数。
     */
    private long rate;

    /**
     * 突发容量。
     */
    private long burst;

    /**
     * 时间窗口大小（秒）。
     */
    private long windowSize;

    /**
     * 限流算法。
     */
    private @Nullable RateLimitAlgorithm algorithm;
}

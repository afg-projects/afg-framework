package io.github.afgprojects.framework.ai.core.properties.resilience;

import lombok.Data;

/**
 * 熔断器配置。
 */
@Data
public class CircuitBreakerConfig {

    /**
     * 熔断器名称。
     */
    private String name = "default";

    /**
     * 滑动窗口大小。
     */
    private int windowSize = 100;

    /**
     * 失败率阈值（0.0 ~ 1.0）。
     */
    private double failureRateThreshold = 0.5;

    /**
     * 半开状态最大探测次数。
     */
    private int halfOpenMaxCalls = 10;

    /**
     * 熔断开启状态超时时间（毫秒）。
     */
    private long openStateTimeoutMs = 30000;
}

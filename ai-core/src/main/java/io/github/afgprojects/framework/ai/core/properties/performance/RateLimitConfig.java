package io.github.afgprojects.framework.ai.core.properties.performance;

import lombok.Data;

/**
 * 限流配置。
 */
@Data
public class RateLimitConfig {

    /**
     * 默认许可数。
     */
    private int defaultPermits = 10;

    /**
     * 限流窗口时间（秒）。
     */
    private long windowSeconds = 60;
}

package io.github.afgprojects.framework.ai.core.properties.performance;

import lombok.Data;

/**
 * 性能配置。
 */
@Data
public class PerformanceConfig {

    /**
     * 是否启用性能优化。
     */
    private boolean enabled = true;

    /**
     * 缓存配置。
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * 限流配置。
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();
}

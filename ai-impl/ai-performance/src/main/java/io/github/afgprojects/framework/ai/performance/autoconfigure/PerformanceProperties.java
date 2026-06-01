package io.github.afgprojects.framework.ai.performance.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI Performance configuration properties.
 *
 * <p>Prefix: {@code afg.ai.performance}
 */
@Data
@ConfigurationProperties(prefix = "afg.ai.performance")
public class PerformanceProperties {

    /**
     * Whether performance optimizations are enabled.
     */
    private boolean enabled = true;

    /**
     * Cache configuration.
     */
    private CacheConfig cache = new CacheConfig();

    /**
     * Rate limiter configuration.
     */
    private RateLimitConfig rateLimit = new RateLimitConfig();

    @Data
    public static class CacheConfig {

        /**
         * Maximum number of entries in the cache.
         */
        private long maxSize = 1000;

        /**
         * Cache entry time-to-live in seconds.
         */
        private long ttlSeconds = 600;
    }

    @Data
    public static class RateLimitConfig {

        /**
         * Default number of permits per window.
         */
        private long defaultPermits = 100;

        /**
         * Window duration in seconds.
         */
        private long windowSeconds = 1;
    }
}

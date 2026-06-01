package io.github.afgprojects.framework.ai.performance.autoconfigure;

import io.github.afgprojects.framework.ai.core.performance.Cache;
import io.github.afgprojects.framework.ai.core.performance.RateLimiter;
import io.github.afgprojects.framework.ai.performance.DefaultCache;
import io.github.afgprojects.framework.ai.performance.DefaultRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * Auto-configuration for AI performance optimizations.
 *
 * <p>Configures caching and rate limiting.
 *
 * @see PerformanceProperties
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(PerformanceProperties.class)
@ConditionalOnClass(DefaultCache.class)
@ConditionalOnProperty(prefix = "afg.ai.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Cache aiCache(PerformanceProperties properties) {
        PerformanceProperties.CacheConfig config = properties.getCache();
        log.info("Creating DefaultCache with maxSize={}, ttlSeconds={}s",
                config.getMaxSize(), config.getTtlSeconds());
        return new DefaultCache(config.getMaxSize(), Duration.ofSeconds(config.getTtlSeconds()));
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter aiRateLimiter(PerformanceProperties properties) {
        PerformanceProperties.RateLimitConfig config = properties.getRateLimit();
        log.info("Creating DefaultRateLimiter with defaultPermits={}, windowSeconds={}s",
                config.getDefaultPermits(), config.getWindowSeconds());
        return new DefaultRateLimiter(config.getDefaultPermits(), Duration.ofSeconds(config.getWindowSeconds()));
    }
}

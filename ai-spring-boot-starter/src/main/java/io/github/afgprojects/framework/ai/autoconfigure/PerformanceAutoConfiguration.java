package io.github.afgprojects.framework.ai.autoconfigure;

import io.github.afgprojects.framework.ai.core.performance.Cache;
import io.github.afgprojects.framework.ai.core.performance.RateLimiter;
import io.github.afgprojects.framework.ai.performance.DefaultCache;
import io.github.afgprojects.framework.ai.performance.DefaultRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

/**
 * 性能优化模块自动配置
 *
 * <p>配置缓存、速率限制器。
 *
 * <p>配置示例：
 * <pre>{@code
 * afg:
 *   ai:
 *     performance:
 *       enabled: true
 *       cache:
 *         enabled: true
 *         max-size: 1000
 *         ttl-seconds: 600
 *       rate-limit:
 *         enabled: true
 *         default-permits: 100
 *         window-seconds: 1
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(AiConfigurationProperties.class)
@ConditionalOnClass({Cache.class, RateLimiter.class})
@ConditionalOnProperty(prefix = "afg.ai.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PerformanceAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAutoConfiguration.class);

    /**
     * 配置缓存
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.performance.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(Cache.class)
    public Cache<String, Object> aiCache(AiConfigurationProperties properties) {
        AiConfigurationProperties.CacheConfig config = properties.getPerformance().getCache();

        log.info("Creating default AI cache: maxSize={}, ttlSeconds={}",
                config.getMaxSize(), config.getTtlSeconds());

        return new DefaultCache<>(config.getMaxSize(), Duration.ofSeconds(config.getTtlSeconds()));
    }

    /**
     * 配置速率限制器
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.ai.performance.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(RateLimiter.class)
    public RateLimiter rateLimiter(AiConfigurationProperties properties) {
        AiConfigurationProperties.RateLimitConfig config = properties.getPerformance().getRateLimit();

        log.info("Creating default rate limiter: defaultPermits={}, windowSeconds={}",
                config.getDefaultPermits(), config.getWindowSeconds());

        return new DefaultRateLimiter(config.getDefaultPermits(), Duration.ofSeconds(config.getWindowSeconds()));
    }
}
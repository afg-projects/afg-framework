package io.github.afgprojects.framework.ai.core.autoconfigure;

import io.github.afgprojects.framework.ai.core.api.performance.Cache;
import io.github.afgprojects.framework.ai.core.api.performance.RateLimiter;
import io.github.afgprojects.framework.ai.core.config.AfgAiProperties;
import io.github.afgprojects.framework.ai.core.performance.DefaultCache;
import io.github.afgprojects.framework.ai.core.performance.DefaultRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AFG AI 性能优化自动配置。
 *
 * <p>配置前缀：{@code afg.ai.performance}
 *
 * @author afg-projects
 * @since 1.0.0
 */
@AutoConfiguration(after = AiCoreAutoConfiguration.class,
        afterName = "io.github.afgprojects.framework.core.autoconfigure.AfgAutoConfiguration")
@EnableConfigurationProperties(AfgAiProperties.class)
@ConditionalOnProperty(prefix = "afg.ai.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiPerformanceAutoConfiguration {

    @Slf4j
    @Configuration
    @ConditionalOnProperty(prefix = "afg.ai.performance", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class PerformanceConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public Cache<?, ?> aiCache(AfgAiProperties properties) {
            AfgAiProperties.PerformanceConfig.CacheConfig config = properties.getPerformance().getCache();
            log.info("Creating DefaultCache with maxSize={}, ttlSeconds={}s",
                    config.getMaxSize(), config.getTtlSeconds());
            return new DefaultCache<>(config.getMaxSize(), Duration.ofSeconds(config.getTtlSeconds()));
        }

        @Bean
        @ConditionalOnMissingBean
        public RateLimiter aiRateLimiter(AfgAiProperties properties) {
            AfgAiProperties.PerformanceConfig.RateLimitConfig config = properties.getPerformance().getRateLimit();
            log.info("Creating DefaultRateLimiter with defaultPermits={}, windowSeconds={}s",
                    config.getDefaultPermits(), config.getWindowSeconds());
            return new DefaultRateLimiter(config.getDefaultPermits(), Duration.ofSeconds(config.getWindowSeconds()));
        }

        // TODO: 阶段4添加 AOP 切面 Bean
        // @Bean
        // @ConditionalOnMissingBean
        // public AiRateLimitedAspect aiRateLimitedAspect(RateLimiter rateLimiter) {
        //     return new AiRateLimitedAspect(rateLimiter);
        // }
    }
}

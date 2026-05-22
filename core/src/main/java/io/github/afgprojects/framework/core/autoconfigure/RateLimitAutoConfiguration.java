package io.github.afgprojects.framework.core.autoconfigure;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.github.afgprojects.framework.core.api.ratelimit.DefaultDimensionResolver;
import io.github.afgprojects.framework.core.api.ratelimit.DefaultWhitelistStrategy;
import io.github.afgprojects.framework.core.api.ratelimit.DimensionResolver;
import io.github.afgprojects.framework.core.api.ratelimit.LocalRateLimitStorage;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitStorage;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimiter;
import io.github.afgprojects.framework.core.api.ratelimit.WhitelistStrategy;
import io.github.afgprojects.framework.core.config.AfgCoreProperties;
import io.github.afgprojects.framework.core.web.ratelimit.RateLimitInterceptor;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 限流自动配置类
 * <p>
 * 自动配置限流功能，支持多种存储后端。
 * 配置项: afg.rate-limit.enabled=true
 * </p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.core.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AfgCoreProperties.class)
public class RateLimitAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public WhitelistStrategy whitelistStrategy(AfgCoreProperties properties) {
        return new DefaultWhitelistStrategy(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DimensionResolver dimensionResolver() {
        return new DefaultDimensionResolver();
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitStorage.class)
    public RateLimitStorage localRateLimitStorage() {
        return new LocalRateLimitStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter(
            RateLimitStorage storage,
            AfgCoreProperties properties,
            WhitelistStrategy whitelistStrategy,
            DimensionResolver dimensionResolver) {
        return new RateLimiter(storage, properties, whitelistStrategy, dimensionResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public RateLimitInterceptor rateLimitInterceptor(
            RateLimiter rateLimiter,
            AfgCoreProperties properties,
            @Nullable MeterRegistry meterRegistry) {
        return new RateLimitInterceptor(rateLimiter, properties, meterRegistry);
    }
}

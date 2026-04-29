package io.github.afgprojects.framework.core.web.ratelimit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;

/**
 * 限流自动配置类
 * <p>
 * 当 Redisson 客户端存在时自动配置限流功能。
 * 配置项: afg.rate-limit.enabled=true
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.redisson.api.RedissonClient")
@ConditionalOnProperty(prefix = "afg.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitAutoConfiguration {

    /**
     * 限流器 Bean
     *
     * @param redissonClient Redisson 客户端（可选，如果为 null 则使用本地限流）
     * @param properties     限流配置属性
     * @return 限流器实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimiter rateLimiter(@Nullable RedissonClient redissonClient, RateLimitProperties properties) {
        return new RateLimiter(redissonClient, properties);
    }

    /**
     * 限流切面 Bean
     *
     * @param rateLimiter   限流器
     * @param properties    限流配置属性
     * @param meterRegistry 指标注册器（可选）
     * @return 限流切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    public RateLimitInterceptor rateLimitInterceptor(RateLimiter rateLimiter,
            RateLimitProperties properties,
            @Nullable MeterRegistry meterRegistry) {
        return new RateLimitInterceptor(rateLimiter, properties, meterRegistry);
    }
}

package io.github.afgprojects.framework.integration.redis.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Redis 限流配置属性
 * <p>
 * 配置项前缀: afg.rate-limit.redis
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "afg.rate-limit.redis")
public class RedisRateLimitProperties {

    /**
     * Redis key 前缀（覆盖通用配置）
     */
    private String keyPrefix;

    /**
     * 是否使用 Redisson 原生限流器
     */
    private boolean useNativeRateLimiter = true;
}

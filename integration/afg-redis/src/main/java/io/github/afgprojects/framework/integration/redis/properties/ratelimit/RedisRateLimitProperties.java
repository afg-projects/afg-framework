package io.github.afgprojects.framework.integration.redis.properties.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Redis 限流配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.rate-limit.redis")
public class RedisRateLimitProperties {

    private String keyPrefix;
    private boolean useNativeRateLimiter = true;
}

package io.github.afgprojects.framework.integration.redis.properties.scheduler;

import java.time.Duration;

import lombok.Data;

/**
 * Redisson 重试配置。
 */
@Data
public class RedissonRetryProperties {

    private boolean enabled = true;
    private int maxAttempts = 3;
    private Duration initialDelay = Duration.ofSeconds(1);
    private double multiplier = 2.0;
}

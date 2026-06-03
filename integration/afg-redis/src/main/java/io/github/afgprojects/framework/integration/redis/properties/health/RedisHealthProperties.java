package io.github.afgprojects.framework.integration.redis.properties.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Redis 健康检查配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.health.redis")
public class RedisHealthProperties {

    private boolean enabled = true;
    private long connectionTimeout = 3000;
    private boolean includeServerInfo = true;
    private long responseTimeWarningThreshold = 1000;
    private long responseTimeCriticalThreshold = 3000;
}

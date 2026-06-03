package io.github.afgprojects.framework.integration.redis.properties.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Redis 缓存配置属性
 */
@Data
@ConfigurationProperties(prefix = "afg.redis.cache")
public class RedisCacheProperties {

    private boolean enabled = true;
    private Duration defaultTtl = Duration.ofMinutes(30);
    private Map<String, Duration> caches = new HashMap<>();
}

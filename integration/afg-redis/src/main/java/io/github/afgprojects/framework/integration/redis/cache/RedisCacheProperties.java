package io.github.afgprojects.framework.integration.redis.cache;

import lombok.Data;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 缓存配置属性
 * <p>
 * 配置前缀: afg.redis.cache
 * </p>
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   redis:
 *     cache:
 *       enabled: true
 *       default-ttl: 30m
 *       caches:
 *         users: 1h
 *         products: 10m
 *         sessions: 24h
 * </pre>
 */
@Data
public class RedisCacheProperties {

    /**
     * 是否启用 Redis 缓存
     */
    private boolean enabled = true;

    /**
     * 全局默认 TTL（默认 30 分钟）
     */
    private Duration defaultTtl = Duration.ofMinutes(30);

    /**
     * 按缓存名配置的 TTL
     * key: 缓存名
     * value: TTL 时长
     */
    private Map<String, Duration> caches = new HashMap<>();
}

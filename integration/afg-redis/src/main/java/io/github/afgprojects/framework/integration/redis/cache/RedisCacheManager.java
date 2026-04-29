package io.github.afgprojects.framework.integration.redis.cache;

import io.github.afgprojects.framework.core.cache.AfgCache;
import io.github.afgprojects.framework.core.cache.CacheManager;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 缓存管理器
 * <p>
 * 实现 {@link CacheManager} 接口，基于 Redis 的分布式缓存管理，支持多缓存实例管理和生命周期控制。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持多缓存实例管理</li>
 *   <li>支持全局默认 TTL 配置</li>
 *   <li>支持按缓存名单独配置 TTL</li>
 *   <li>线程安全的缓存实例创建</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * RedisCacheManager manager = new RedisCacheManager(redissonClient, Duration.ofMinutes(30));
 * AfgCache&lt;User&gt; userCache = manager.getCache("users");
 * AfgCache&lt;Product&gt; productCache = manager.getCache("products", Product.class);
 * </pre>
 */
public class RedisCacheManager implements CacheManager {

    private final RedissonClient redissonClient;
    private final long defaultTtlMillis;
    private final Map<String, Long> cacheTtls;
    private final Map<String, AfgCache<?>> caches = new ConcurrentHashMap<>();

    /**
     * 构造缓存管理器
     *
     * @param redissonClient Redisson 客户端
     * @param defaultTtl     全局默认过期时间
     */
    public RedisCacheManager(RedissonClient redissonClient, Duration defaultTtl) {
        this.redissonClient = redissonClient;
        this.defaultTtlMillis = defaultTtl != null ? defaultTtl.toMillis() : 0;
        this.cacheTtls = new ConcurrentHashMap<>();
    }

    /**
     * 构造缓存管理器，支持按缓存名配置 TTL
     *
     * @param redissonClient Redisson 客户端
     * @param defaultTtl     全局默认过期时间
     * @param cacheTtls      按缓存名配置的 TTL 映射
     */
    public RedisCacheManager(RedissonClient redissonClient, Duration defaultTtl, Map<String, Duration> cacheTtls) {
        this.redissonClient = redissonClient;
        this.defaultTtlMillis = defaultTtl != null ? defaultTtl.toMillis() : 0;
        this.cacheTtls = new ConcurrentHashMap<>();
        if (cacheTtls != null) {
            cacheTtls.forEach((name, ttl) -> this.cacheTtls.put(name, ttl.toMillis()));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public <V> AfgCache<V> getCache(@NonNull String name) {
        return (AfgCache<V>) caches.computeIfAbsent(name, this::createCache);
    }

    @Override
    @NonNull
    public <V> AfgCache<V> getCache(@NonNull String name, Class<V> type) {
        // type 参数用于类型安全检查，实际缓存实例与 getCache(name) 相同
        return getCache(name);
    }

    @Override
    public void destroy() {
        caches.values().forEach(AfgCache::clear);
        caches.clear();
    }

    /**
     * 添加缓存 TTL 配置
     *
     * @param cacheName 缓存名称
     * @param ttl       过期时间
     */
    public void addCacheTtl(String cacheName, Duration ttl) {
        cacheTtls.put(cacheName, ttl.toMillis());
    }

    /**
     * 获取指定缓存的 TTL 配置
     *
     * @param cacheName 缓存名称
     * @return 该缓存的 TTL（毫秒），未配置则返回全局默认 TTL
     */
    public long getCacheTtl(String cacheName) {
        return cacheTtls.getOrDefault(cacheName, defaultTtlMillis);
    }

    private AfgCache<?> createCache(String name) {
        long ttl = getCacheTtl(name);
        return new RedisCache<>(redissonClient, name, ttl);
    }
}
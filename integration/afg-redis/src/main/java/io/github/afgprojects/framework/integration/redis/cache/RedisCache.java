package io.github.afgprojects.framework.integration.redis.cache;

import io.github.afgprojects.framework.core.cache.AfgCache;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存实现
 * <p>
 * 基于 Redisson RMapCache 实现的分布式缓存，支持 TTL 过期时间。
 * 实现 {@link AfgCache} 接口。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持默认 TTL 配置</li>
 *   <li>支持单条数据的自定义 TTL</li>
 *   <li>线程安全，适合分布式环境</li>
 * </ul>
 *
 * <h3>使用示例</h3>
 * <pre>
 * RedisCache&lt;User&gt; userCache = new RedisCache&lt;&gt;(redissonClient, "users", Duration.ofMinutes(30));
 * userCache.put("user:123", user);
 * User cached = userCache.get("user:123");
 * </pre>
 *
 * @param <V> 缓存值类型
 */
public class RedisCache<V> implements AfgCache<V> {

    private final String name;
    private final RMapCache<String, V> mapCache;
    private final long defaultTtlMillis;

    /**
     * 构造 Redis 缓存实例
     *
     * @param redissonClient Redisson 客户端
     * @param name           缓存名称（用于构建 Redis key 前缀）
     * @param defaultTtl     默认过期时间（毫秒），小于等于 0 表示永不过期
     */
    public RedisCache(RedissonClient redissonClient, String name, long defaultTtlMillis) {
        this.name = name;
        this.mapCache = redissonClient.getMapCache("afg:cache:" + name);
        this.defaultTtlMillis = defaultTtlMillis;
    }

    /**
     * 构造 Redis 缓存实例
     *
     * @param redissonClient Redisson 客户端
     * @param name           缓存名称（用于构建 Redis key 前缀）
     * @param defaultTtl     默认过期时间，为 null 或零表示永不过期
     */
    public RedisCache(RedissonClient redissonClient, String name, java.time.Duration defaultTtl) {
        this(redissonClient, name, defaultTtl != null ? defaultTtl.toMillis() : 0);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @Nullable
    public V get(@NonNull String key) {
        return mapCache.get(key);
    }

    @Override
    public void put(@NonNull String key, @NonNull V value) {
        if (defaultTtlMillis > 0) {
            mapCache.put(key, value, defaultTtlMillis, TimeUnit.MILLISECONDS);
        } else {
            mapCache.put(key, value);
        }
    }

    @Override
    public void put(@NonNull String key, @NonNull V value, long ttlMillis) {
        if (ttlMillis > 0) {
            mapCache.put(key, value, ttlMillis, TimeUnit.MILLISECONDS);
        } else {
            mapCache.put(key, value);
        }
    }

    @Override
    public void evict(@NonNull String key) {
        mapCache.remove(key);
    }

    @Override
    public void clear() {
        mapCache.clear();
    }

    @Override
    @Nullable
    public V putIfAbsent(@NonNull String key, @NonNull V value, long ttlMillis) {
        if (ttlMillis > 0) {
            return mapCache.putIfAbsent(key, value, ttlMillis, TimeUnit.MILLISECONDS);
        }
        return mapCache.putIfAbsent(key, value);
    }

    @Override
    public boolean containsKey(@NonNull String key) {
        return mapCache.containsKey(key);
    }

    @Override
    public long size() {
        return mapCache.size();
    }
}
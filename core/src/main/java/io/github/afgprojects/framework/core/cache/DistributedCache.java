package io.github.afgprojects.framework.core.cache;

import java.time.Duration;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.cache.metrics.CacheMetrics;

/**
 * 分布式缓存实现
 * <p>
 * 基于 Redisson 的分布式缓存，支持集群环境下的数据共享
 * </p>
 *
 * @param <V> 缓存值类型
 */
public class DistributedCache<V> implements LoadingCache<V> {

    /**
     * 缓存名称
     */
    private final String name;

    /**
     * 缓存配置
     */
    private final CacheConfig config;

    /**
     * Redisson 客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 缓存指标
     */
    private final CacheMetrics metrics;

    /**
     * 缓存键前缀
     */
    private final String keyPrefix;

    /**
     * 构造分布式缓存
     *
     * @param name           缓存名称
     * @param config         缓存配置
     * @param redissonClient Redisson 客户端
     */
    public DistributedCache(@NonNull String name, @NonNull CacheConfig config, @NonNull RedissonClient redissonClient) {
        this.name = name;
        this.config = config;
        this.redissonClient = redissonClient;
        this.keyPrefix = "afg:cache:" + name + ":";
        this.metrics = new CacheMetrics(name, "distributed");
    }

    /**
     * 构建完整的缓存键
     *
     * @param key 原始键
     * @return 完整键
     */
    private String buildKey(String key) {
        return keyPrefix + key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public V get(String key) {
        metrics.recordGet();
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        Object value = bucket.get();
        if (value == null) {
            metrics.recordMiss();
            return null;
        }
        metrics.recordHit();
        return value == NullValue.INSTANCE ? null : (V) value;
    }

    @Override
    public void put(String key, V value) {
        put(key, value, config.getDefaultTtl());
    }

    @Override
    public void put(String key, V value, long ttlMillis) {
        metrics.recordPut();
        Object cacheValue = wrapValue(value);
        if (cacheValue == null) {
            // 不缓存 null 且值为 null，直接跳过
            return;
        }
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        if (ttlMillis > 0) {
            bucket.set(cacheValue, Duration.ofMillis(ttlMillis));
        } else if (config.getDefaultTtl() > 0) {
            bucket.set(cacheValue, Duration.ofMillis(config.getDefaultTtl()));
        } else {
            bucket.set(cacheValue);
        }
    }

    @Override
    public void evict(String key) {
        metrics.recordEviction();
        redissonClient.getBucket(buildKey(key)).delete();
    }

    @Override
    public void clear() {
        metrics.recordClear();
        // 删除该缓存的所有键
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(keyPrefix + "*");
        for (String redisKey : keys) {
            redissonClient.getBucket(redisKey).delete();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public V putIfAbsent(String key, V value, long ttlMillis) {
        metrics.recordGet();
        Object cacheValue = wrapValue(value);
        if (cacheValue == null) {
            metrics.recordMiss();
            return null;
        }
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        boolean success;
        if (ttlMillis > 0) {
            success = bucket.setIfAbsent(cacheValue, Duration.ofMillis(ttlMillis));
        } else if (config.getDefaultTtl() > 0) {
            success = bucket.setIfAbsent(cacheValue, Duration.ofMillis(config.getDefaultTtl()));
        } else {
            success = bucket.setIfAbsent(cacheValue);
        }
        if (success) {
            metrics.recordMiss();
            metrics.recordPut();
            return null;
        }
        // 获取已存在的值
        Object existing = bucket.get();
        metrics.recordHit();
        return existing == NullValue.INSTANCE ? null : (V) existing;
    }

    @Override
    public boolean containsKey(String key) {
        return redissonClient.getBucket(buildKey(key)).isExists();
    }

    @Override
    public long size() {
        // 分布式缓存无法高效获取大小，返回预估值为 -1
        long count = 0;
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(keyPrefix + "*");
        for (String ignored : keys) {
            count++;
        }
        return count;
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader) {
        return getOrLoad(key, loader, config.getDefaultTtl());
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader, long ttlMillis) {
        metrics.recordGet();
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        Object value = bucket.get();
        if (value != null) {
            metrics.recordHit();
            return value == NullValue.INSTANCE ? null : castValue(value);
        }
        metrics.recordMiss();
        // 加载数据
        V loadedValue = loader.get();
        metrics.recordLoad();
        put(key, loadedValue, ttlMillis);
        return loadedValue;
    }

    /**
     * 包装缓存值
     */
    private Object wrapValue(V value) {
        if (value == null && config.isCacheNull()) {
            return NullValue.INSTANCE;
        }
        return value;
    }

    /**
     * 转换缓存值类型
     */
    @SuppressWarnings("unchecked")
    private V castValue(Object value) {
        return (V) value;
    }

    /**
     * 获取缓存配置
     *
     * @return 缓存配置
     */
    public CacheConfig getConfig() {
        return config;
    }

    /**
     * 获取缓存指标
     *
     * @return 缓存指标
     */
    @NonNull
    public CacheMetrics getMetrics() {
        return metrics;
    }

    /**
     * 获取 Redisson 客户端
     *
     * @return Redisson 客户端
     */
    @NonNull
    public RedissonClient getRedissonClient() {
        return redissonClient;
    }

    /**
     * 创建默认分布式缓存
     *
     * @param name           缓存名称
     * @param redissonClient Redisson 客户端
     * @return 分布式缓存实例
     */
    @NonNull
    public static <V> DistributedCache<V> createDefault(@NonNull String name, @NonNull RedissonClient redissonClient) {
        return new DistributedCache<>(name, CacheConfig.defaultConfig(), redissonClient);
    }
}
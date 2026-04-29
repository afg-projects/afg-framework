package io.github.afgprojects.framework.core.cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import io.github.afgprojects.framework.core.cache.metrics.CacheMetrics;

/**
 * 本地缓存实现
 * <p>
 * 基于 Caffeine 的高性能本地缓存
 * 支持容量限制、过期策略、统计信息
 * </p>
 *
 * @param <V> 缓存值类型
 */
public class LocalCache<V> implements LoadingCache<V> {

    /**
     * 缓存名称
     */
    private final String name;

    /**
     * 缓存配置
     */
    private final CacheConfig config;

    /**
     * Caffeine 缓存实例
     */
    private final Cache<String, Object> cache;

    /**
     * 缓存指标
     */
    private final CacheMetrics metrics;

    /**
     * 构造本地缓存
     *
     * @param name   缓存名称
     * @param config 缓存配置
     */
    public LocalCache(@NonNull String name, @NonNull CacheConfig config) {
        this.name = name;
        this.config = config;
        this.metrics = new CacheMetrics(name, "local");
        this.cache = buildCache(config);
    }

    /**
     * 构建 Caffeine 缓存
     */
    @SuppressWarnings("unchecked")
    private Cache<String, Object> buildCache(CacheConfig config) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        // 设置初始容量
        builder.initialCapacity(config.getInitialCapacity());

        // 设置最大容量
        builder.maximumSize(config.getMaximumSize());

        // 设置过期策略
        if (config.getExpireAfterWrite() != null) {
            builder.expireAfterWrite(config.getExpireAfterWrite());
        } else if (config.getExpireAfterAccess() != null) {
            builder.expireAfterAccess(config.getExpireAfterAccess());
        } else if (config.getDefaultTtl() > 0) {
            // 使用自定义过期策略，支持每个条目独立的 TTL
            builder.expireAfter(new Expiry<String, Object>() {
                @Override
                public long expireAfterCreate(String key, Object value, long currentTimestamp) {
                    // NullValue 使用特殊过期时间
                    if (value == NullValue.INSTANCE) {
                        return config.getNullValueTtl() * 1_000_000L; // 转换为纳秒
                    }
                    return config.getDefaultTtl() * 1_000_000L; // 转换为纳秒
                }

                @Override
                public long expireAfterUpdate(String key, Object value, long currentTimestamp, long currentDuration) {
                    // NullValue 使用特殊过期时间
                    if (value == NullValue.INSTANCE) {
                        return config.getNullValueTtl() * 1_000_000L; // 转换为纳秒
                    }
                    return config.getDefaultTtl() * 1_000_000L; // 转换为纳秒
                }

                @Override
                public long expireAfterRead(String key, Object value, long currentTimestamp, long currentDuration) {
                    // 读取不改变过期时间
                    return currentDuration;
                }
            });
        }

        // 开启统计
        if (config.isRecordStats()) {
            builder.recordStats();
        }

        Cache<?, ?> rawCache = builder.build();
        return (Cache<String, Object>) rawCache;
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
        Object value = cache.getIfPresent(key);
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
        Object cacheValue = value == null && config.isCacheNull() ? NullValue.INSTANCE : value;
        if (cacheValue == null) {
            // 不缓存 null 且值为 null，直接跳过
            return;
        }
        if (ttlMillis > 0) {
            // Caffeine 不支持单条目 TTL，使用默认过期策略
            cache.put(key, cacheValue);
        } else {
            cache.put(key, cacheValue);
        }
    }

    @Override
    public void evict(String key) {
        metrics.recordEviction();
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        metrics.recordClear();
        cache.invalidateAll();
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public V putIfAbsent(String key, V value, long ttlMillis) {
        metrics.recordGet();
        Object existing = cache.asMap().putIfAbsent(key, wrapValue(value));
        if (existing == null) {
            metrics.recordMiss();
            metrics.recordPut();
            return null;
        }
        metrics.recordHit();
        return existing == NullValue.INSTANCE ? null : (V) existing;
    }

    @Override
    public boolean containsKey(String key) {
        Object value = cache.getIfPresent(key);
        return value != null;
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader) {
        return getOrLoad(key, loader, config.getDefaultTtl());
    }

    @Override
    @SuppressWarnings("unchecked")
    public V getOrLoad(String key, Supplier<V> loader, long ttlMillis) {
        metrics.recordGet();
        Object value = cache.get(key, k -> {
            metrics.recordMiss();
            V loadedValue = loader.get();
            metrics.recordLoad();
            return wrapValue(loadedValue);
        });
        metrics.recordHit();
        return value == NullValue.INSTANCE ? null : (V) value;
    }

    /**
     * 包装缓存值
     */
    private Object wrapValue(V value) {
        return value == null && config.isCacheNull() ? NullValue.INSTANCE : value;
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
     * 获取 Caffeine 统计信息
     *
     * @return 统计信息
     */
    public com.github.benmanes.caffeine.cache.stats.CacheStats getCacheStats() {
        return cache.stats();
    }

    /**
     * 创建默认本地缓存
     *
     * @param name 缓存名称
     * @return 本地缓存实例
     */
    @NonNull
    public static <V> LocalCache<V> createDefault(@NonNull String name) {
        return new LocalCache<>(name, CacheConfig.defaultConfig());
    }
}
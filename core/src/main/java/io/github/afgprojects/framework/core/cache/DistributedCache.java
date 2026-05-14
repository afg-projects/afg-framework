package io.github.afgprojects.framework.core.cache;

import java.time.Duration;
import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.cache.metrics.CacheMetrics;
import io.github.afgprojects.framework.core.cache.spi.DistributedCacheStorage;

/**
 * 分布式缓存实现
 * <p>
 * 基于分布式缓存存储的缓存实现，支持集群环境下的数据共享。
 * </p>
 */
public class DistributedCache<V> implements LoadingCache<V> {

    private final String name;
    private final CacheConfig config;
    private final DistributedCacheStorage storage;
    private final CacheMetrics metrics;
    private final String keyPrefix;

    public DistributedCache(@NonNull String name, @NonNull CacheConfig config,
                            @NonNull DistributedCacheStorage storage) {
        this.name = name;
        this.config = config;
        this.storage = storage;
        this.keyPrefix = "afg:cache:" + name + ":";
        this.metrics = new CacheMetrics(name, "distributed");
    }

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
        Object value = storage.get(buildKey(key));
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
            return;
        }
        Duration ttl = ttlMillis > 0 ? Duration.ofMillis(ttlMillis) :
                      config.getDefaultTtl() > 0 ? Duration.ofMillis(config.getDefaultTtl()) : null;
        if (ttl != null) {
            storage.set(buildKey(key), cacheValue, ttl);
        } else {
            storage.set(buildKey(key), cacheValue);
        }
    }

    @Override
    public void evict(String key) {
        metrics.recordEviction();
        storage.delete(buildKey(key));
    }

    @Override
    public void clear() {
        metrics.recordClear();
        storage.deleteByPattern(keyPrefix + "*");
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
        Duration ttl = ttlMillis > 0 ? Duration.ofMillis(ttlMillis) :
                      config.getDefaultTtl() > 0 ? Duration.ofMillis(config.getDefaultTtl()) : null;
        boolean success = storage.setIfAbsent(buildKey(key), cacheValue, ttl);
        if (success) {
            metrics.recordMiss();
            metrics.recordPut();
            return null;
        }
        Object existing = storage.get(buildKey(key));
        metrics.recordHit();
        return existing == NullValue.INSTANCE ? null : (V) existing;
    }

    @Override
    public boolean containsKey(String key) {
        return storage.exists(buildKey(key));
    }

    @Override
    public long size() {
        return storage.countByPattern(keyPrefix + "*");
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader) {
        return getOrLoad(key, loader, config.getDefaultTtl());
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader, long ttlMillis) {
        metrics.recordGet();
        Object value = storage.get(buildKey(key));
        if (value != null) {
            metrics.recordHit();
            return value == NullValue.INSTANCE ? null : castValue(value);
        }
        metrics.recordMiss();
        V loadedValue = loader.get();
        metrics.recordLoad();
        put(key, loadedValue, ttlMillis);
        return loadedValue;
    }

    private Object wrapValue(V value) {
        if (value == null && config.isCacheNull()) {
            return NullValue.INSTANCE;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private V castValue(Object value) {
        return (V) value;
    }

    public CacheConfig getConfig() {
        return config;
    }

    @NonNull
    public CacheMetrics getMetrics() {
        return metrics;
    }

    public DistributedCacheStorage getStorage() {
        return storage;
    }

    @NonNull
    public static <V> DistributedCache<V> createDefault(@NonNull String name,
                                                         @NonNull DistributedCacheStorage storage) {
        return new DistributedCache<>(name, CacheConfig.defaultConfig(), storage);
    }
}

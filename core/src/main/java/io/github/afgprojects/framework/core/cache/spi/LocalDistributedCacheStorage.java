package io.github.afgprojects.framework.core.cache.spi;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jspecify.annotations.Nullable;

/**
 * 本地分布式缓存存储实现
 * <p>
 * 基于 JVM 内存的分布式缓存实现，仅适用于单机部署或测试场景。
 * 注意：此实现不支持集群环境下的数据共享。
 */
public class LocalDistributedCacheStorage implements DistributedCacheStorage {

    private final ConcurrentMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final String cacheName;

    public LocalDistributedCacheStorage(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    @Nullable
    public Object get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key, entry);
            return null;
        }
        return entry.value;
    }

    @Override
    public void set(String key, Object value) {
        cache.put(key, new CacheEntry(value, null));
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        long expireTime = ttl != null ? System.currentTimeMillis() + ttl.toMillis() : 0;
        cache.put(key, new CacheEntry(value, expireTime));
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        long expireTime = ttl != null ? System.currentTimeMillis() + ttl.toMillis() : 0;
        CacheEntry entry = new CacheEntry(value, expireTime);
        return cache.putIfAbsent(key, entry) == null;
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public boolean exists(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            cache.remove(key, entry);
            return false;
        }
        return true;
    }

    @Override
    public void deleteByPattern(String pattern) {
        String regex = pattern.replace("*", ".*");
        cache.keySet().removeIf(key -> key.matches(regex));
    }

    @Override
    public long countByPattern(String pattern) {
        String regex = pattern.replace("*", ".*");
        return cache.keySet().stream()
                .filter(key -> key.matches(regex))
                .count();
    }

    private static class CacheEntry {
        final Object value;
        final long expireTime; // 0 表示永不过期

        CacheEntry(Object value, Long expireTime) {
            this.value = value;
            this.expireTime = expireTime != null ? expireTime : 0;
        }

        boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }
    }
}

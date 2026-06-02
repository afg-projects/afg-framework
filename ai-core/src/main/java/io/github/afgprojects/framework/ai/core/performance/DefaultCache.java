package io.github.afgprojects.framework.ai.core.performance;

import io.github.afgprojects.framework.ai.core.api.performance.Cache;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认缓存实现
 *
 * <p>基于内存的 LRU 缓存，适用于：
 * <ul>
 *   <li>开发测试环境</li>
 *   <li>单机部署</li>
 *   <li>轻量级缓存需求</li>
 * </ul>
 *
 * <p>生产环境建议使用 Redis 或 Caffeine 实现。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultCache<K, V> implements Cache<K, V> {

    private static final Logger log = LoggerFactory.getLogger(DefaultCache.class);

    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final LinkedHashMap<K, CacheEntry<V>> lruQueue;
    private final ReentrantLock lock = new ReentrantLock();

    private final long maxSize;
    private final Duration defaultTtl;

    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong loadSuccessCount = new AtomicLong(0);
    private final AtomicLong loadFailureCount = new AtomicLong(0);
    private final AtomicLong totalLoadTime = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    /**
     * 创建默认缓存
     *
     * @param maxSize    最大容量
     * @param defaultTtl 默认过期时间
     */
    public DefaultCache(long maxSize, Duration defaultTtl) {
        this.maxSize = maxSize;
        this.defaultTtl = defaultTtl;

        // LRU 队列
        this.lruQueue = new LinkedHashMap<>(16, 0.75f, true);
    }

    /**
     * 创建默认缓存（默认 1000 条，永不过期）
     */
    public DefaultCache() {
        this(1000, null);
    }

    @Override
    @NonNull
    public Optional<V> get(@NonNull K key) {
        CacheEntry<V> entry = cache.get(key);

        if (entry == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        if (entry.isExpired()) {
            remove(key);
            missCount.incrementAndGet();
            return Optional.empty();
        }

        hitCount.incrementAndGet();

        // 更新 LRU
        lock.lock();
        try {
            lruQueue.put(key, entry);
        } finally {
            lock.unlock();
        }

        return Optional.of(entry.getValue());
    }

    @Override
    @NonNull
    public V get(@NonNull K key, @NonNull CacheLoader<K, V> loader) {
        return get(key, loader, defaultTtl != null ? defaultTtl : Duration.ofMillis(Long.MAX_VALUE));
    }

    @Override
    @NonNull
    public V get(@NonNull K key, @NonNull CacheLoader<K, V> loader, @NonNull Duration ttl) {
        Optional<V> cached = get(key);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 加载值
        long startTime = System.nanoTime();
        try {
            V value = loader.load(key);
            long loadTime = System.nanoTime() - startTime;

            loadSuccessCount.incrementAndGet();
            totalLoadTime.addAndGet(loadTime);

            put(key, value, ttl);

            return value;
        } catch (Exception e) {
            loadFailureCount.incrementAndGet();
            throw new RuntimeException("Failed to load cache value for key: " + key, e);
        }
    }

    @Override
    public void put(@NonNull K key, @NonNull V value) {
        put(key, value, defaultTtl != null ? defaultTtl : Duration.ofMillis(Long.MAX_VALUE));
    }

    @Override
    public void put(@NonNull K key, @NonNull V value, @NonNull Duration ttl) {
        long expiresAt = ttl != null && !ttl.isNegative() && ttl.toMillis() < Long.MAX_VALUE
                ? System.currentTimeMillis() + ttl.toMillis()
                : Long.MAX_VALUE;

        DefaultCacheEntry<V> entry = new DefaultCacheEntry<>(value, System.currentTimeMillis(), expiresAt);

        lock.lock();
        try {
            cache.put(key, entry);
            lruQueue.put(key, entry);

            // 驱逐最久未使用的条目
            while (cache.size() > maxSize) {
                Map.Entry<K, CacheEntry<V>> eldest = lruQueue.entrySet().iterator().next();
                if (eldest != null) {
                    cache.remove(eldest.getKey());
                    lruQueue.remove(eldest.getKey());
                    evictionCount.incrementAndGet();
                    log.debug("Evicted cache entry for key: {}", eldest.getKey());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    @Nullable
    public V putIfAbsent(@NonNull K key, @NonNull V value) {
        CacheEntry<V> existing = cache.get(key);
        if (existing != null && !existing.isExpired()) {
            return existing.getValue();
        }

        put(key, value);
        return null;
    }

    @Override
    @NonNull
    public Optional<V> remove(@NonNull K key) {
        lock.lock();
        try {
            CacheEntry<V> entry = cache.remove(key);
            lruQueue.remove(key);

            if (entry != null) {
                return Optional.of(entry.getValue());
            }
            return Optional.empty();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsKey(@NonNull K key) {
        CacheEntry<V> entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            cache.clear();
            lruQueue.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    @NonNull
    public CacheStats getStats() {
        return new DefaultCacheStats(
                hitCount.get(),
                missCount.get(),
                loadSuccessCount.get(),
                loadFailureCount.get(),
                totalLoadTime.get(),
                evictionCount.get(),
                cache.size(),
                maxSize
        );
    }

    @Override
    public long invalidate(@NonNull CachePredicate<K, V> predicate) {
        long count = 0;

        lock.lock();
        try {
            Iterator<Map.Entry<K, CacheEntry<V>>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<K, CacheEntry<V>> entry = iterator.next();
                if (predicate.test(entry.getKey(), entry.getValue().getValue())) {
                    iterator.remove();
                    lruQueue.remove(entry.getKey());
                    count++;
                }
            }
        } finally {
            lock.unlock();
        }

        return count;
    }

    /**
     * 默认缓存条目
     */
    private static class DefaultCacheEntry<V> implements CacheEntry<V> {
        private final V value;
        private final long createdAt;
        private final long expiresAt;
        private final AtomicLong accessCount = new AtomicLong(0);
        private volatile long lastAccessTime;
        private final Map<String, String> metadata = new HashMap<>();

        DefaultCacheEntry(V value, long createdAt, long expiresAt) {
            this.value = value;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.lastAccessTime = createdAt;
        }

        @Override
        @NonNull
        public V getValue() { return value; }

        @Override
        public long getCreatedAt() { return createdAt; }

        @Override
        @Nullable
        public Long getExpiresAt() {
            return expiresAt < Long.MAX_VALUE ? expiresAt : null;
        }

        @Override
        public boolean isExpired() {
            return expiresAt < Long.MAX_VALUE && System.currentTimeMillis() > expiresAt;
        }

        @Override
        public long getAccessCount() { return accessCount.get(); }

        @Override
        public long getLastAccessTime() { return lastAccessTime; }

        @Override
        @NonNull
        public Map<String, String> getMetadata() { return metadata; }

        void recordAccess() {
            accessCount.incrementAndGet();
            lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * 默认缓存统计
     */
    private static class DefaultCacheStats implements CacheStats {
        private final long hitCount;
        private final long missCount;
        private final long loadSuccessCount;
        private final long loadFailureCount;
        private final long totalLoadTime;
        private final long evictionCount;
        private final long size;
        private final long maxSize;

        DefaultCacheStats(long hitCount, long missCount, long loadSuccessCount, long loadFailureCount,
                          long totalLoadTime, long evictionCount, long size, long maxSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.loadSuccessCount = loadSuccessCount;
            this.loadFailureCount = loadFailureCount;
            this.totalLoadTime = totalLoadTime;
            this.evictionCount = evictionCount;
            this.size = size;
            this.maxSize = maxSize;
        }

        @Override
        public long getHitCount() { return hitCount; }

        @Override
        public long getMissCount() { return missCount; }

        @Override
        public double getHitRate() {
            long total = hitCount + missCount;
            return total > 0 ? (double) hitCount / total : 0.0;
        }

        @Override
        public long getLoadSuccessCount() { return loadSuccessCount; }

        @Override
        public long getLoadFailureCount() { return loadFailureCount; }

        @Override
        public double getAverageLoadTime() {
            long loads = loadSuccessCount + loadFailureCount;
            return loads > 0 ? (double) totalLoadTime / loads : 0.0;
        }

        @Override
        public long getEvictionCount() { return evictionCount; }

        @Override
        public long getSize() { return size; }

        @Override
        public long getMaxSize() { return maxSize; }
    }
}

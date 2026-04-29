package io.github.afgprojects.framework.core.cache;

import java.util.function.Supplier;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.cache.metrics.CacheMetrics;

/**
 * 多级缓存实现
 * <p>
 * 组合本地缓存和分布式缓存，实现两级缓存策略：
 * <ol>
 *   <li>L1: 本地缓存（Caffeine），快速访问</li>
 *   <li>L2: 分布式缓存（Redis），数据共享</li>
 * </ol>
 * 查询顺序：L1 -> L2 -> 数据源
 * 写入顺序：L2 -> L1（先写分布式缓存确保数据一致性）
 * </p>
 *
 * @param <V> 缓存值类型
 */
public class MultiLevelCache<V> implements LoadingCache<V> {

    /**
     * 缓存名称
     */
    private final String name;

    /**
     * 本地缓存（L1）
     */
    private final LocalCache<V> localCache;

    /**
     * 分布式缓存（L2）
     */
    private final DistributedCache<V> distributedCache;

    /**
     * 缓存指标
     */
    private final CacheMetrics metrics;

    /**
     * 是否同步本地缓存过期
     */
    private final boolean syncLocalExpiry;

    /**
     * 构造多级缓存
     *
     * @param name              缓存名称
     * @param localCache        本地缓存
     * @param distributedCache  分布式缓存
     */
    public MultiLevelCache(
            @NonNull String name,
            @NonNull LocalCache<V> localCache,
            @NonNull DistributedCache<V> distributedCache) {
        this(name, localCache, distributedCache, true);
    }

    /**
     * 构造多级缓存
     *
     * @param name              缓存名称
     * @param localCache        本地缓存
     * @param distributedCache  分布式缓存
     * @param syncLocalExpiry   是否同步本地缓存过期
     */
    public MultiLevelCache(
            @NonNull String name,
            @NonNull LocalCache<V> localCache,
            @NonNull DistributedCache<V> distributedCache,
            boolean syncLocalExpiry) {
        this.name = name;
        this.localCache = localCache;
        this.distributedCache = distributedCache;
        this.syncLocalExpiry = syncLocalExpiry;
        this.metrics = new CacheMetrics(name, "multi-level");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @Nullable
    public V get(String key) {
        metrics.recordGet();

        // 1. 先查本地缓存（L1）
        V value = localCache.get(key);
        if (value != null) {
            metrics.recordHit();
            return value;
        }

        // 2. 本地未命中，查分布式缓存（L2）
        value = distributedCache.get(key);
        if (value != null) {
            metrics.recordHit();
            // 回填本地缓存
            localCache.put(key, value);
            return value;
        }

        metrics.recordMiss();
        return null;
    }

    @Override
    public void put(String key, V value) {
        put(key, value, 0);
    }

    @Override
    public void put(String key, V value, long ttlMillis) {
        metrics.recordPut();
        // 先写分布式缓存（确保数据一致性）
        distributedCache.put(key, value, ttlMillis);
        // 再写本地缓存
        localCache.put(key, value, ttlMillis);
    }

    @Override
    public void evict(String key) {
        metrics.recordEviction();
        // 先删除本地缓存
        localCache.evict(key);
        // 再删除分布式缓存
        distributedCache.evict(key);
    }

    @Override
    public void clear() {
        metrics.recordClear();
        // 先清空本地缓存
        localCache.clear();
        // 再清空分布式缓存
        distributedCache.clear();
    }

    @Override
    @Nullable
    public V putIfAbsent(String key, V value, long ttlMillis) {
        metrics.recordGet();
        // 先检查分布式缓存
        V existing = distributedCache.putIfAbsent(key, value, ttlMillis);
        if (existing == null) {
            metrics.recordMiss();
            metrics.recordPut();
            // 写入成功，同时写入本地缓存
            localCache.put(key, value, ttlMillis);
            return null;
        }
        metrics.recordHit();
        return existing;
    }

    @Override
    public boolean containsKey(String key) {
        // 先查本地缓存
        if (localCache.containsKey(key)) {
            return true;
        }
        // 再查分布式缓存
        return distributedCache.containsKey(key);
    }

    @Override
    public long size() {
        // 返回本地缓存大小，因为分布式缓存大小获取成本高
        return localCache.size();
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader) {
        return getOrLoad(key, loader, 0);
    }

    @Override
    public V getOrLoad(String key, Supplier<V> loader, long ttlMillis) {
        metrics.recordGet();

        // 1. 先查本地缓存
        V value = localCache.get(key);
        if (value != null) {
            metrics.recordHit();
            return value;
        }

        // 2. 查分布式缓存
        value = distributedCache.get(key);
        if (value != null) {
            metrics.recordHit();
            // 回填本地缓存
            localCache.put(key, value, ttlMillis);
            return value;
        }

        // 3. 都未命中，加载数据
        metrics.recordMiss();
        value = loader.get();
        metrics.recordLoad();

        // 4. 回填两级缓存
        if (value != null) {
            put(key, value, ttlMillis);
        }

        return value;
    }

    /**
     * 获取本地缓存
     *
     * @return 本地缓存
     */
    @NonNull
    public LocalCache<V> getLocalCache() {
        return localCache;
    }

    /**
     * 获取分布式缓存
     *
     * @return 分布式缓存
     */
    @NonNull
    public DistributedCache<V> getDistributedCache() {
        return distributedCache;
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
     * 仅清除本地缓存
     * <p>
     * 用于本地缓存过期或需要强制刷新的场景
     * </p>
     *
     * @param key 缓存键
     */
    public void evictLocal(String key) {
        localCache.evict(key);
    }

    /**
     * 清除所有本地缓存
     */
    public void clearLocal() {
        localCache.clear();
    }

    /**
     * 创建多级缓存
     *
     * @param name           缓存名称
     * @param config         缓存配置
     * @param redissonClient Redisson 客户端
     * @return 多级缓存实例
     */
    @NonNull
    public static <V> MultiLevelCache<V> create(
            @NonNull String name,
            @NonNull CacheConfig config,
            org.redisson.api.RedissonClient redissonClient) {
        LocalCache<V> localCache = new LocalCache<>(name, config);
        DistributedCache<V> distributedCache = new DistributedCache<>(name, config, redissonClient);
        return new MultiLevelCache<>(name, localCache, distributedCache);
    }
}
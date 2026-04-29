package io.github.afgprojects.framework.core.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.cache.exception.CacheException;

/**
 * 默认缓存管理器
 * <p>
 * 实现 {@link CacheManager} 接口，统一管理缓存实例的创建、获取和销毁。
 * 支持本地缓存、分布式缓存和多级缓存
 * </p>
 */
public class DefaultCacheManager implements CacheManager {

    /**
     * 缓存实例映射
     */
    private final Map<String, AfgCache<?>> cacheMap = new ConcurrentHashMap<>();

    /**
     * 缓存配置属性
     */
    private final CacheProperties properties;

    /**
     * Redisson 客户端（可选，用于分布式缓存）
     */
    private final RedissonClient redissonClient;

    /**
     * 构造缓存管理器
     *
     * @param properties 缓存配置属性
     */
    public DefaultCacheManager(@NonNull CacheProperties properties) {
        this(properties, null);
    }

    /**
     * 构造缓存管理器
     *
     * @param properties     缓存配置属性
     * @param redissonClient Redisson 客户端（可选）
     */
    public DefaultCacheManager(@NonNull CacheProperties properties, @Nullable RedissonClient redissonClient) {
        this.properties = properties;
        this.redissonClient = redissonClient;
    }

    /**
     * 获取缓存实例
     * <p>
     * 如果缓存不存在，根据配置自动创建
     * </p>
     *
     * @param cacheName 缓存名称
     * @param <V>       缓存值类型
     * @return 缓存实例
     */
    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public <V> AfgCache<V> getCache(@NonNull String cacheName) {
        return (AfgCache<V>) cacheMap.computeIfAbsent(cacheName, this::createCache);
    }

    /**
     * 获取缓存实例（指定类型）
     * <p>
     * type 参数用于类型安全检查，实际缓存实例与 getCache(name) 相同
     * </p>
     *
     * @param cacheName 缓存名称
     * @param type      缓存值类型
     * @param <V>       缓存值类型
     * @return 缓存实例
     */
    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public <V> AfgCache<V> getCache(@NonNull String cacheName, Class<V> type) {
        // type 参数用于类型安全检查，实际缓存实例与 getCache(name) 相同
        return getCache(cacheName);
    }

    /**
     * 获取缓存实例（指定缓存类型）
     *
     * @param cacheName 缓存名称
     * @param type      缓存类型
     * @param <V>       缓存值类型
     * @return 缓存实例
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public <V> AfgCache<V> getCache(@NonNull String cacheName, CacheProperties.CacheType type) {
        String key = cacheName + ":" + type.name();
        return (AfgCache<V>) cacheMap.computeIfAbsent(key, k -> createCache(cacheName, type));
    }

    /**
     * 获取本地缓存
     *
     * @param cacheName 缓存名称
     * @param <V>       缓存值类型
     * @return 本地缓存实例
     */
    @NonNull
    public <V> LocalCache<V> getLocalCache(@NonNull String cacheName) {
        AfgCache<V> cache = getCache(cacheName, CacheProperties.CacheType.LOCAL);
        if (cache instanceof LocalCache) {
            return (LocalCache<V>) cache;
        }
        throw new CacheException("Cache '" + cacheName + "' is not a LocalCache");
    }

    /**
     * 获取分布式缓存
     *
     * @param cacheName 缓存名称
     * @param <V>       缓存值类型
     * @return 分布式缓存实例
     */
    @NonNull
    public <V> DistributedCache<V> getDistributedCache(@NonNull String cacheName) {
        if (redissonClient == null) {
            throw new CacheException("RedissonClient is not configured, cannot create distributed cache");
        }
        AfgCache<V> cache = getCache(cacheName, CacheProperties.CacheType.DISTRIBUTED);
        if (cache instanceof DistributedCache) {
            return (DistributedCache<V>) cache;
        }
        throw new CacheException("Cache '" + cacheName + "' is not a DistributedCache");
    }

    /**
     * 获取多级缓存
     *
     * @param cacheName 缓存名称
     * @param <V>       缓存值类型
     * @return 多级缓存实例
     */
    @NonNull
    public <V> MultiLevelCache<V> getMultiLevelCache(@NonNull String cacheName) {
        if (redissonClient == null) {
            throw new CacheException("RedissonClient is not configured, cannot create multi-level cache");
        }
        AfgCache<V> cache = getCache(cacheName, CacheProperties.CacheType.MULTI_LEVEL);
        if (cache instanceof MultiLevelCache) {
            return (MultiLevelCache<V>) cache;
        }
        throw new CacheException("Cache '" + cacheName + "' is not a MultiLevelCache");
    }

    /**
     * 创建缓存实例
     *
     * @param cacheName 缓存名称
     * @return 缓存实例
     */
    @NonNull
    private AfgCache<?> createCache(@NonNull String cacheName) {
        return createCache(cacheName, properties.getType());
    }

    /**
     * 创建指定类型的缓存实例
     *
     * @param cacheName 缓存名称
     * @param type      缓存类型
     * @return 缓存实例
     */
    @NonNull
    private AfgCache<?> createCache(@NonNull String cacheName, CacheProperties.CacheType type) {
        CacheConfig config = properties.getCacheConfig(cacheName);

        switch (type) {
            case LOCAL:
                return new LocalCache<>(cacheName, config);
            case DISTRIBUTED:
                if (redissonClient == null) {
                    throw new CacheException("RedissonClient is not configured, cannot create distributed cache");
                }
                return new DistributedCache<>(cacheName, config, redissonClient);
            case MULTI_LEVEL:
                if (redissonClient == null) {
                    throw new CacheException("RedissonClient is not configured, cannot create multi-level cache");
                }
                return MultiLevelCache.create(cacheName, config, redissonClient);
            default:
                throw new CacheException("Unknown cache type: " + type);
        }
    }

    /**
     * 注册缓存实例
     *
     * @param cacheName 缓存名称
     * @param cache     缓存实例
     */
    public void registerCache(@NonNull String cacheName, @NonNull AfgCache<?> cache) {
        cacheMap.put(cacheName, cache);
    }

    /**
     * 移除缓存实例
     *
     * @param cacheName 缓存名称
     */
    public void removeCache(@NonNull String cacheName) {
        AfgCache<?> cache = cacheMap.remove(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 检查缓存是否存在
     *
     * @param cacheName 缓存名称
     * @return 存在返回 true
     */
    public boolean containsCache(@NonNull String cacheName) {
        return cacheMap.containsKey(cacheName);
    }

    /**
     * 获取所有缓存名称
     *
     * @return 缓存名称集合
     */
    public java.util.Set<String> getCacheNames() {
        return new java.util.HashSet<>(cacheMap.keySet());
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        cacheMap.values().forEach(AfgCache::clear);
    }

    /**
     * 销毁所有缓存
     */
    @Override
    public void destroy() {
        clearAll();
        cacheMap.clear();
    }

    /**
     * 获取缓存配置属性
     *
     * @return 缓存配置属性
     */
    @NonNull
    public CacheProperties getProperties() {
        return properties;
    }
}

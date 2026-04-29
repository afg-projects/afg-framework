package io.github.afgprojects.framework.data.jdbc.cache;

import io.github.afgprojects.framework.core.cache.CacheConfig;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体缓存管理器
 * <p>
 * 管理所有实体类型的缓存实例，提供统一的缓存获取和失效管理。
 * </p>
 */
public class EntityCacheManager {

    private final DefaultCacheManager cacheManager;
    private final EntityCacheProperties properties;
    private final Map<Class<?>, EntityCache<?>> cacheMap = new ConcurrentHashMap<>();

    /**
     * 构造实体缓存管理器
     *
     * @param cacheManager 缓存管理器
     * @param properties   实体缓存配置
     */
    public EntityCacheManager(@NonNull DefaultCacheManager cacheManager,
                              @NonNull EntityCacheProperties properties) {
        this.cacheManager = cacheManager;
        this.properties = properties;
    }

    /**
     * 获取实体缓存
     * <p>
     * 如果缓存不存在，根据配置自动创建
     * </p>
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return 实体缓存实例
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> EntityCache<T> getCache(@NonNull Class<T> entityClass) {
        return (EntityCache<T>) cacheMap.computeIfAbsent(entityClass, this::createCache);
    }

    /**
     * 获取实体缓存（如果存在）
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     * @return 实体缓存实例，不存在时返回 null
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> EntityCache<T> getCacheIfPresent(@NonNull Class<T> entityClass) {
        return (EntityCache<T>) cacheMap.get(entityClass);
    }

    /**
     * 失效单个实体缓存
     *
     * @param entityClass 实体类型
     * @param id          实体 ID
     * @param <T>         实体类型
     */
    public <T> void evict(@NonNull Class<T> entityClass, @NonNull Object id) {
        EntityCache<T> cache = getCacheIfPresent(entityClass);
        if (cache != null) {
            cache.evict(id);
        }
    }

    /**
     * 失效指定实体类型的所有缓存
     *
     * @param entityClass 实体类型
     * @param <T>         实体类型
     */
    public <T> void evictAll(@NonNull Class<T> entityClass) {
        EntityCache<T> cache = getCacheIfPresent(entityClass);
        if (cache != null) {
            cache.evictAll();
        }
    }

    /**
     * 失效所有实体缓存
     */
    public void evictAllCaches() {
        cacheMap.values().forEach(EntityCache::evictAll);
    }

    /**
     * 清空所有缓存
     */
    public void clearAll() {
        cacheMap.values().forEach(EntityCache::clear);
    }

    /**
     * 检查是否启用缓存
     *
     * @return 启用返回 true
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }

    /**
     * 获取实体缓存配置
     *
     * @return 实体缓存配置
     */
    @NonNull
    public EntityCacheProperties getProperties() {
        return properties;
    }

    /**
     * 创建实体缓存
     *
     * @param entityClass 实体类型
     * @return 实体缓存实例
     */
    @NonNull
    private <T> EntityCache<T> createCache(@NonNull Class<T> entityClass) {
        CacheConfig config = buildCacheConfig();
        return new DefaultEntityCache<>(entityClass, cacheManager, config);
    }

    /**
     * 构建缓存配置
     *
     * @return 缓存配置
     */
    @NonNull
    private CacheConfig buildCacheConfig() {
        CacheConfig config = new CacheConfig();
        config.defaultTtl(properties.getTtl());
        config.maximumSize(properties.getMaxSize());
        config.cacheNull(properties.isCacheNull());
        config.nullValueTtl(properties.getNullValueTtl());
        return config;
    }
}

package io.github.afgprojects.framework.data.jdbc.cache;

import io.github.afgprojects.framework.core.cache.AfgCache;
import io.github.afgprojects.framework.core.cache.CacheConfig;
import io.github.afgprojects.framework.core.cache.DefaultCacheManager;
import io.github.afgprojects.framework.core.cache.LocalCache;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 默认实体缓存实现
 * <p>
 * 基于 {@link DefaultCacheManager} 实现实体二级缓存，
 * 支持缓存过期时间配置和最大缓存数量限制。
 * </p>
 */
public class DefaultEntityCache<T> implements EntityCache<T> {

    private static final String CACHE_KEY_PREFIX = "entity:";

    private final Class<T> entityClass;
    private final String cacheName;
    private final AfgCache<T> cache;

    /**
     * 构造实体缓存
     *
     * @param entityClass     实体类型
     * @param cacheManager    缓存管理器
     * @param cacheConfigName 缓存配置名称（用于从 CacheProperties 获取特定配置）
     */
    public DefaultEntityCache(@NonNull Class<T> entityClass,
                              @NonNull DefaultCacheManager cacheManager,
                              @Nullable String cacheConfigName) {
        this.entityClass = entityClass;
        this.cacheName = CACHE_KEY_PREFIX + entityClass.getName();
        this.cache = cacheManager.getCache(cacheName);
    }

    /**
     * 构造实体缓存（使用默认配置）
     *
     * @param entityClass  实体类型
     * @param cacheManager 缓存管理器
     */
    public DefaultEntityCache(@NonNull Class<T> entityClass,
                              @NonNull DefaultCacheManager cacheManager) {
        this.entityClass = entityClass;
        this.cacheName = CACHE_KEY_PREFIX + entityClass.getName();
        this.cache = cacheManager.getCache(cacheName);
    }

    /**
     * 构造实体缓存（使用自定义配置）
     *
     * @param entityClass  实体类型
     * @param cacheManager 缓存管理器
     * @param config       缓存配置
     */
    public DefaultEntityCache(@NonNull Class<T> entityClass,
                              @NonNull DefaultCacheManager cacheManager,
                              @NonNull CacheConfig config) {
        this.entityClass = entityClass;
        this.cacheName = CACHE_KEY_PREFIX + entityClass.getName();
        // 注册自定义配置的缓存
        cacheManager.registerCache(cacheName, new LocalCache<>(cacheName, config));
        this.cache = cacheManager.getCache(cacheName);
    }

    @Override
    @NonNull
    public Optional<T> get(@NonNull Object id) {
        String key = buildKey(id);
        T value = cache.get(key);
        return Optional.ofNullable(value);
    }

    @Override
    public void put(@NonNull Object id, @NonNull T entity) {
        String key = buildKey(id);
        cache.put(key, entity);
    }

    @Override
    public void put(@NonNull Object id, @NonNull T entity, long ttlMillis) {
        String key = buildKey(id);
        cache.put(key, entity, ttlMillis);
    }

    @Override
    public void evict(@NonNull Object id) {
        String key = buildKey(id);
        cache.evict(key);
    }

    @Override
    public void evictAll() {
        cache.clear();
    }

    @Override
    public boolean containsKey(@NonNull Object id) {
        String key = buildKey(id);
        return cache.containsKey(key);
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    @NonNull
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    @NonNull
    public String getCacheName() {
        return cacheName;
    }

    /**
     * 构建缓存键
     *
     * @param id 实体 ID
     * @return 缓存键
     */
    private String buildKey(Object id) {
        return String.valueOf(id);
    }
}

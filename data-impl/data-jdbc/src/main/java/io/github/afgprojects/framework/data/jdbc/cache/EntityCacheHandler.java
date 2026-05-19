package io.github.afgprojects.framework.data.jdbc.cache;

import io.github.afgprojects.framework.data.core.context.TenantContextHolder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * 实体缓存处理器
 * <p>
 * 封装实体缓存操作逻辑，包括缓存读取、写入、失效等操作。
 * 从 JdbcEntityProxy 中提取，降低类的复杂度。
 * <p>
 * <strong>多租户支持：</strong>缓存键包含租户ID，确保不同租户的数据隔离。
 *
 * @param <T> 实体类型
 */
public class EntityCacheHandler<T> {

    private final Class<T> entityClass;
    private final @Nullable EntityCacheManager cacheManager;
    private final @Nullable TenantContextHolder tenantContextHolder;

    public EntityCacheHandler(Class<T> entityClass, @Nullable EntityCacheManager cacheManager) {
        this(entityClass, cacheManager, null);
    }

    public EntityCacheHandler(Class<T> entityClass, @Nullable EntityCacheManager cacheManager,
                              @Nullable TenantContextHolder tenantContextHolder) {
        this.entityClass = entityClass;
        this.cacheManager = cacheManager;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * 检查缓存是否启用
     *
     * @return 启用返回 true
     */
    public boolean isCacheEnabled() {
        return cacheManager != null && cacheManager.isEnabled();
    }

    /**
     * 生成包含租户ID的缓存键
     * <p>
     * 缓存键格式：{tenantId}:{id}
     * <ul>
     *   <li>如果租户ID为空，使用空字符串作为前缀</li>
     *   <li>确保多租户场景下的数据隔离</li>
     * </ul>
     *
     * @param id 实体 ID
     * @return 缓存键
     */
    private @NonNull Object buildCacheKey(@NonNull Object id) {
        String tenantId = getTenantId();
        // 使用空字符串作为默认值，避免 null 导致的 NPE
        String tenantPrefix = (tenantId != null && !tenantId.isEmpty()) ? tenantId : "";
        return tenantPrefix + ":" + id;
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID，可能为 null
     */
    private @Nullable String getTenantId() {
        if (tenantContextHolder != null) {
            return tenantContextHolder.getTenantId();
        }
        return null;
    }

    /**
     * 获取用于测试验证的缓存键
     * <p>
     * 此方法主要用于测试场景，允许外部获取实际的缓存键以验证缓存行为。
     *
     * @param id 实体 ID
     * @return 缓存键
     */
    public @NonNull Object getCacheKey(@NonNull Object id) {
        return buildCacheKey(id);
    }

    /**
     * 从缓存获取实体
     *
     * @param id 实体 ID
     * @return 缓存结果，包含命中状态和实体（可能为空）
     */
    public CacheResult<T> get(@NonNull Object id) {
        if (!isCacheEnabled()) {
            return CacheResult.miss();
        }

        Object cacheKey = buildCacheKey(id);
        EntityCache<T> cache = cacheManager.getCache(entityClass);
        Optional<T> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return CacheResult.hit(cached.get());
        }

        // 检查缓存中是否有 null 标记（防穿透）
        if (cache.containsKey(cacheKey)) {
            return CacheResult.nullHit();
        }

        return CacheResult.miss();
    }

    /**
     * 将实体写入缓存
     *
     * @param id     实体 ID
     * @param entity 实体对象
     */
    public void put(@NonNull Object id, @Nullable T entity) {
        if (!isCacheEnabled()) {
            return;
        }

        Object cacheKey = buildCacheKey(id);
        EntityCache<T> cache = cacheManager.getCache(entityClass);
        if (entity != null) {
            cache.put(cacheKey, entity);
        } else if (cacheManager.getProperties().isCacheNull()) {
            // 缓存 null 标记以防止缓存穿透
            cache.put(cacheKey, null);
        }
    }

    /**
     * 缓存查询结果（支持 null 标记防穿透）
     *
     * @param id       实体 ID
     * @param entity   实体对象（可能为 null）
     * @param onCached 命中缓存时的回调
     */
    public void cacheResult(@NonNull Object id, @Nullable T entity, @NonNull Consumer<T> onCached) {
        if (!isCacheEnabled()) {
            return;
        }

        Object cacheKey = buildCacheKey(id);
        EntityCache<T> cache = cacheManager.getCache(entityClass);
        if (entity != null) {
            cache.put(cacheKey, entity);
            onCached.accept(entity);
        } else if (cacheManager.getProperties().isCacheNull()) {
            // 缓存 null 标记以防止缓存穿透
            cache.put(cacheKey, null);
        }
    }

    /**
     * 失效指定 ID 的缓存
     *
     * @param id 实体 ID
     */
    public void evict(@NonNull Object id) {
        if (!isCacheEnabled()) {
            return;
        }

        Object cacheKey = buildCacheKey(id);
        EntityCache<T> cache = cacheManager.getCacheIfPresent(entityClass);
        if (cache != null) {
            cache.evict(cacheKey);
        }
    }

    /**
     * 清除该实体类型的所有缓存
     */
    public void clear() {
        if (!isCacheEnabled()) {
            return;
        }

        EntityCache<T> cache = cacheManager.getCacheIfPresent(entityClass);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 获取缓存管理器
     *
     * @return 缓存管理器，可能为 null
     */
    public @Nullable EntityCacheManager getCacheManager() {
        return cacheManager;
    }

    /**
     * 缓存查询结果
     */
    public static final class CacheResult<T> {
        private final boolean hit;
        private final boolean nullHit;
        private final T entity;

        private CacheResult(boolean hit, boolean nullHit, T entity) {
            this.hit = hit;
            this.nullHit = nullHit;
            this.entity = entity;
        }

        /**
         * 创建缓存命中结果
         */
        public static <T> CacheResult<T> hit(@NonNull T entity) {
            return new CacheResult<>(true, false, entity);
        }

        /**
         * 创建 null 标记命中结果（防穿透）
         */
        public static <T> CacheResult<T> nullHit() {
            return new CacheResult<>(true, true, null);
        }

        /**
         * 创建缓存未命中结果
         */
        public static <T> CacheResult<T> miss() {
            return new CacheResult<>(false, false, null);
        }

        /**
         * 是否命中缓存
         */
        public boolean isHit() {
            return hit;
        }

        /**
         * 是否命中 null 标记
         */
        public boolean isNullHit() {
            return nullHit;
        }

        /**
         * 获取缓存的实体
         */
        public Optional<T> getEntity() {
            return Optional.ofNullable(entity);
        }
    }
}

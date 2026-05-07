package io.github.afgprojects.framework.data.jdbc.cache;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 实体缓存接口
 * <p>
 * 提供实体二级缓存操作，支持按实体类型和 ID 进行缓存管理。
 * 与 ORM 层集成，提供透明的缓存能力。
 * </p>
 *
 * @param <T> 实体类型
 */
public interface EntityCache<T> {

    /**
     * NULL 标记对象，用于防止缓存穿透
     */
    Object NULL_MARKER = new Object();

    /**
     * 从缓存获取实体
     *
     * @param id 实体 ID
     * @return 缓存的实体，不存在时返回 Optional.empty()
     */
    @NonNull Optional<T> get(@NonNull Object id);

    /**
     * 批量获取实体
     *
     * @param ids 实体 ID 集合
     * @return 缓存的实体映射（不存在的 ID 不会包含在结果中）
     */
    default @NonNull Map<Object, T> getAll(@NonNull Set<Object> ids) {
        java.util.Map<Object, T> result = new java.util.HashMap<>();
        for (Object id : ids) {
            get(id).ifPresent(entity -> result.put(id, entity));
        }
        return result;
    }

    /**
     * 缓存实体
     *
     * @param id     实体 ID
     * @param entity 实体对象
     */
    void put(@NonNull Object id, @NonNull T entity);

    /**
     * 缓存实体（指定 TTL）
     *
     * @param id        实体 ID
     * @param entity    实体对象
     * @param ttlMillis 过期时间（毫秒）
     */
    void put(@NonNull Object id, @NonNull T entity, long ttlMillis);

    /**
     * 缓存 null 标记（防止缓存穿透）
     *
     * @param id 实体 ID
     */
    default void putNull(@NonNull Object id) {
        // 默认实现：子类可覆盖以支持 null 标记
    }

    /**
     * 缓存 null 标记（指定 TTL）
     *
     * @param id        实体 ID
     * @param ttlMillis 过期时间（毫秒）
     */
    default void putNull(@NonNull Object id, long ttlMillis) {
        putNull(id);
    }

    /**
     * 检查是否为 null 标记
     *
     * @param id 实体 ID
     * @return 是 null 标记返回 true
     */
    default boolean isNullMarker(@NonNull Object id) {
        return false;
    }

    /**
     * 批量缓存实体
     *
     * @param entities 实体列表
     */
    default void putAll(@NonNull List<T> entities) {
        // 子类需覆盖实现
    }

    /**
     * 失效单个实体缓存
     *
     * @param id 实体 ID
     */
    void evict(@NonNull Object id);

    /**
     * 批量失效缓存
     *
     * @param ids 实体 ID 集合
     */
    default void evictAll(@NonNull Set<Object> ids) {
        for (Object id : ids) {
            evict(id);
        }
    }

    /**
     * 失效该实体类型的所有缓存
     */
    void evictAll();

    /**
     * 检查缓存是否包含指定实体
     *
     * @param id 实体 ID
     * @return 存在返回 true
     */
    boolean containsKey(@NonNull Object id);

    /**
     * 获取缓存大小
     *
     * @return 缓存条目数量
     */
    long size();

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 获取实体类型
     *
     * @return 实体类型
     */
    @NonNull Class<T> getEntityClass();

    /**
     * 获取缓存名称
     *
     * @return 缓存名称
     */
    @NonNull String getCacheName();

    /**
     * 获取所有缓存的 ID
     *
     * @return ID 集合
     */
    default Set<Object> keys() {
        throw new UnsupportedOperationException("keys() not supported");
    }

    /**
     * 获取所有缓存的实体
     *
     * @return 实体集合
     */
    default Collection<T> values() {
        throw new UnsupportedOperationException("values() not supported");
    }
}

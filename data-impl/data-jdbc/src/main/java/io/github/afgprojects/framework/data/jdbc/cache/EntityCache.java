package io.github.afgprojects.framework.data.jdbc.cache;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

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
     * 从缓存获取实体
     *
     * @param id 实体 ID
     * @return 缓存的实体，不存在时返回 Optional.empty()
     */
    @NonNull Optional<T> get(@NonNull Object id);

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
     * 失效单个实体缓存
     *
     * @param id 实体 ID
     */
    void evict(@NonNull Object id);

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
}

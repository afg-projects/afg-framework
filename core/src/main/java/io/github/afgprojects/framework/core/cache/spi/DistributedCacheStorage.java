package io.github.afgprojects.framework.core.cache.spi;

import java.time.Duration;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * 分布式缓存存储接口
 * <p>
 * 定义分布式缓存的核心操作，支持多种存储后端实现。
 */
public interface DistributedCacheStorage {

    /**
     * 获取存储类型
     *
     * @return 存储类型标识（如 redis, hazelcast）
     */
    String getStorageType();

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，不存在返回 null
     */
    @Nullable
    Object get(String key);

    /**
     * 设置缓存值
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void set(String key, Object value);

    /**
     * 设置缓存值（带过期时间）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param ttl     过期时间
     */
    void set(String key, Object value, Duration ttl);

    /**
     * 设置缓存值（不存在时）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param ttl     过期时间
     * @return 是否设置成功
     */
    boolean setIfAbsent(String key, Object value, Duration ttl);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 检查键是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 删除匹配模式的所有键
     *
     * @param pattern 键模式
     */
    void deleteByPattern(String pattern);

    /**
     * 获取匹配模式的键数量
     *
     * @param pattern 键模式
     * @return 键数量
     */
    long countByPattern(String pattern);
}

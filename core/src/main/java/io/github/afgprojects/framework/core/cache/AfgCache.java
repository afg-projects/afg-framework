package io.github.afgprojects.framework.core.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * AFG 缓存接口
 * <p>
 * 定义统一的缓存操作接口，支持本地缓存、分布式缓存和多级缓存
 * </p>
 *
 * @param <V> 缓存值类型
 */
public interface AfgCache<V> {

    /**
     * 获取缓存名称
     *
     * @return 缓存名称
     */
    String getName();

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值，不存在时返回 null
     */
    V get(String key);

    /**
     * 批量获取缓存值
     *
     * @param keys 缓存键集合
     * @return 缓存键值映射（不存在的键不会包含在结果中）
     */
    default Map<String, V> getAll(Set<String> keys) {
        java.util.Map<String, V> result = new java.util.HashMap<>();
        for (String key : keys) {
            V value = get(key);
            if (value != null) {
                result.put(key, value);
            }
        }
        return result;
    }

    /**
     * 存入缓存（使用默认 TTL）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(String key, V value);

    /**
     * 存入缓存（指定 TTL）
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param ttlMillis 过期时间（毫秒），小于等于 0 表示永不过期
     */
    void put(String key, V value, long ttlMillis);

    /**
     * 批量存入缓存（使用默认 TTL）
     *
     * @param map 键值映射
     */
    default void putAll(Map<String, V> map) {
        for (Map.Entry<String, V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * 批量存入缓存（指定 TTL）
     *
     * @param map       键值映射
     * @param ttlMillis 过期时间（毫秒）
     */
    default void putAll(Map<String, V> map, long ttlMillis) {
        for (Map.Entry<String, V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue(), ttlMillis);
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存键
     */
    void evict(String key);

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     */
    default void evictAll(Set<String> keys) {
        for (String key : keys) {
            evict(key);
        }
    }

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 如果不存在则存入缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 已存在的值，如果不存在则返回 null 并存入新值
     */
    default V putIfAbsent(String key, V value) {
        return putIfAbsent(key, value, 0);
    }

    /**
     * 如果不存在则存入缓存（指定 TTL）
     *
     * @param key       缓存键
     * @param value     缓存值
     * @param ttlMillis 过期时间（毫秒），小于等于 0 表示永不过期
     * @return 已存在的值，如果不存在则返回 null 并存入新值
     */
    V putIfAbsent(String key, V value, long ttlMillis);

    /**
     * 检查缓存是否存在
     *
     * @param key 缓存键
     * @return 存在返回 true
     */
    boolean containsKey(String key);

    /**
     * 获取缓存大小
     *
     * @return 缓存条目数量
     */
    long size();

    /**
     * 获取所有缓存键
     *
     * @return 缓存键集合
     */
    default Set<String> keys() {
        throw new UnsupportedOperationException("keys() not supported");
    }

    /**
     * 获取所有缓存值
     *
     * @return 缓存值集合
     */
    default Collection<V> values() {
        throw new UnsupportedOperationException("values() not supported");
    }
}

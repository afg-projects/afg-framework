package io.github.afgprojects.framework.core.cache;

import java.util.function.Supplier;

/**
 * AFG 缓存接口扩展
 * <p>
 * 提供 getOrLoad 方法，支持缓存未命中时自动加载
 * </p>
 *
 * @param <V> 缓存值类型
 */
public interface LoadingCache<V> extends AfgCache<V> {

    /**
     * 获取缓存值，未命中时自动加载
     *
     * @param key   缓存键
     * @param loader 数据加载器
     * @return 缓存值或加载的值
     */
    V getOrLoad(String key, Supplier<V> loader);

    /**
     * 获取缓存值，未命中时自动加载（指定 TTL）
     *
     * @param key       缓存键
     * @param loader    数据加载器
     * @param ttlMillis 过期时间（毫秒）
     * @return 缓存值或加载的值
     */
    V getOrLoad(String key, Supplier<V> loader, long ttlMillis);
}

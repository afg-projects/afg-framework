package io.github.afgprojects.framework.core.cache.spi;

import org.jspecify.annotations.Nullable;

/**
 * 缓存存储提供者接口
 * <p>
 * 用于创建分布式缓存存储实例。
 */
public interface CacheStorageProvider {

    /**
     * 获取存储类型
     *
     * @return 存储类型标识
     */
    String getStorageType();

    /**
     * 创建分布式缓存存储
     *
     * @param cacheName 缓存名称
     * @param keyPrefix  键前缀
     * @return 分布式缓存存储实例
     */
    DistributedCacheStorage createStorage(String cacheName, String keyPrefix);

    /**
     * 检查是否可用
     *
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
}

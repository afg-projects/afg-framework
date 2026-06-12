package io.github.afgprojects.framework.core.cache.spi;

/**
 * NoOp 缓存存储提供者
 * <p>
 * 空操作降级实现，创建的存储实例不执行任何实际缓存操作。
 * 适用于未配置 Redis 等分布式缓存后端的场景。
 * <p>
 * 由 {@code CacheAutoConfiguration} 在无其他 {@link CacheStorageProvider} 实现时自动注册。
 *
 * @since 1.0.0
 */
public class NoOpCacheStorageProvider implements CacheStorageProvider {

    @Override
    public String getStorageType() {
        return "noop";
    }

    @Override
    public DistributedCacheStorage createStorage(String cacheName, String keyPrefix) {
        return new NoOpDistributedCacheStorage();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}

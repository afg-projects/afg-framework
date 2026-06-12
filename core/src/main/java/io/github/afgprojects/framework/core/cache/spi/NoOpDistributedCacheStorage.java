package io.github.afgprojects.framework.core.cache.spi;

import java.time.Duration;

import org.jspecify.annotations.Nullable;

/**
 * NoOp 分布式缓存存储实现
 * <p>
 * 空操作降级实现，所有写操作被忽略，所有读操作返回 null/false/0。
 * 适用于未配置 Redis 等分布式缓存后端的场景。
 * <p>
 * 由 {@link NoOpCacheStorageProvider} 创建，不直接注册为 Bean。
 *
 * @since 1.0.0
 */
public class NoOpDistributedCacheStorage implements DistributedCacheStorage {

    @Override
    public String getStorageType() {
        return "noop";
    }

    @Override
    @Nullable
    public Object get(String key) {
        return null;
    }

    @Override
    public void set(String key, Object value) {
        // no-op
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        // no-op
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        return false;
    }

    @Override
    public void delete(String key) {
        // no-op
    }

    @Override
    public boolean exists(String key) {
        return false;
    }

    @Override
    public void deleteByPattern(String pattern) {
        // no-op
    }

    @Override
    public long countByPattern(String pattern) {
        return 0;
    }
}

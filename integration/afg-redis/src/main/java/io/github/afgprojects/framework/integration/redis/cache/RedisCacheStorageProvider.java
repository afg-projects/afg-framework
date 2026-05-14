package io.github.afgprojects.framework.integration.redis.cache;

import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.cache.spi.CacheStorageProvider;
import io.github.afgprojects.framework.core.cache.spi.DistributedCacheStorage;

/**
 * Redis 缓存存储提供者
 */
public class RedisCacheStorageProvider implements CacheStorageProvider {

    private final RedissonClient redissonClient;

    public RedisCacheStorageProvider(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public String getStorageType() {
        return "redis";
    }

    @Override
    public DistributedCacheStorage createStorage(String cacheName, String keyPrefix) {
        return new RedisDistributedCacheStorage(redissonClient, keyPrefix);
    }

    @Override
    public boolean isAvailable() {
        try {
            redissonClient.getBucket("__health_check__").get();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

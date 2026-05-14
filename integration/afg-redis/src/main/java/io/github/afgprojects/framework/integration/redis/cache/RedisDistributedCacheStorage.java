package io.github.afgprojects.framework.integration.redis.cache;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import io.github.afgprojects.framework.core.cache.spi.DistributedCacheStorage;

/**
 * Redis 分布式缓存存储实现
 */
public class RedisDistributedCacheStorage implements DistributedCacheStorage {

    private final RedissonClient redissonClient;
    private final String keyPrefix;

    public RedisDistributedCacheStorage(RedissonClient redissonClient, String keyPrefix) {
        this.redissonClient = redissonClient;
        this.keyPrefix = keyPrefix;
    }

    private String buildKey(String key) {
        return keyPrefix + key;
    }

    @Override
    public String getStorageType() {
        return "redis";
    }

    @Override
    @Nullable
    public Object get(String key) {
        RBucket<Object> bucket = redissonClient.getBucket(buildKey(key));
        return bucket.get();
    }

    @Override
    public void set(String key, Object value) {
        redissonClient.getBucket(buildKey(key)).set(value);
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        redissonClient.getBucket(buildKey(key)).set(value, ttl);
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        return redissonClient.getBucket(buildKey(key)).setIfAbsent(value, ttl);
    }

    @Override
    public void delete(String key) {
        redissonClient.getBucket(buildKey(key)).delete();
    }

    @Override
    public boolean exists(String key) {
        return redissonClient.getBucket(buildKey(key)).isExists();
    }

    @Override
    public void deleteByPattern(String pattern) {
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(pattern);
        for (String key : keys) {
            redissonClient.getBucket(key).delete();
        }
    }

    @Override
    public long countByPattern(String pattern) {
        long count = 0;
        Iterable<String> keys = redissonClient.getKeys().getKeysByPattern(pattern);
        for (String ignored : keys) {
            count++;
        }
        return count;
    }
}

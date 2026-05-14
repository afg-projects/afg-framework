package io.github.afgprojects.framework.integration.redis.ratelimit;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RAtomicLong;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.ratelimit.RateLimitResult;
import io.github.afgprojects.framework.core.api.ratelimit.RateLimitStorage;

/**
 * Redis 限流存储实现
 * <p>
 * 使用 Redisson 原生限流器实现高性能分布式限流。
 * </p>
 */
public class RedisRateLimitStorage implements RateLimitStorage {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimitStorage.class);

    private final RedissonClient redissonClient;
    private final RedisRateLimitProperties properties;

    /**
     * 构造函数
     *
     * @param redissonClient Redisson 客户端
     * @param properties      Redis 限流配置
     */
    public RedisRateLimitStorage(RedissonClient redissonClient, RedisRateLimitProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public String getStorageType() {
        return "redis";
    }

    @Override
    public RateLimitResult tryAcquireTokenBucket(String key, long rate, long burst) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, rate, 1, RateIntervalUnit.SECONDS);

        boolean acquired = rateLimiter.tryAcquire(1);
        long remaining = acquired ? Math.max(0, burst - 1) : 0;

        if (acquired) {
            return RateLimitResult.allowed(remaining, burst, System.currentTimeMillis() + 1000);
        } else {
            return RateLimitResult.rejected(burst, System.currentTimeMillis() + 1000, 1000 / rate + 1);
        }
    }

    @Override
    public RateLimitResult tryAcquireSlidingWindow(String key, long rate, long windowSize) {
        long windowSizeMs = windowSize * 1000;
        long now = System.currentTimeMillis();
        long windowStart = now - windowSizeMs;

        String counterKey = key + ":counter";
        String timestampKey = key + ":timestamp";

        try {
            RAtomicLong counter = redissonClient.getAtomicLong(counterKey);
            RAtomicLong timestamp = redissonClient.getAtomicLong(timestampKey);

            Long lastTimestamp = timestamp.get();

            if (lastTimestamp == null || lastTimestamp < windowStart) {
                // 窗口过期，重置计数器
                counter.set(1);
                timestamp.set(now);
                counter.expire(windowSize, TimeUnit.SECONDS);
                timestamp.expire(windowSize, TimeUnit.SECONDS);
                return RateLimitResult.allowed(rate - 1, rate, now + windowSizeMs);
            }

            long currentCount = counter.get();

            if (currentCount < rate) {
                // 在限流范围内，增加计数
                counter.incrementAndGet();
                return RateLimitResult.allowed(rate - currentCount - 1, rate, lastTimestamp + windowSizeMs);
            } else {
                // 超过限流
                long retryAfter = lastTimestamp + windowSizeMs - now;
                return RateLimitResult.rejected(rate, lastTimestamp + windowSizeMs, retryAfter);
            }
        } catch (Exception e) {
            log.error("Sliding window rate limiter error for key: {}", key, e);
            return RateLimitResult.allowed(rate, rate, now + windowSizeMs);
        }
    }

    @Override
    public long increment(String key, long delta, long ttl) {
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        long result = atomic.addAndGet(delta);
        atomic.expire(ttl, TimeUnit.SECONDS);
        return result;
    }

    @Override
    public boolean compareAndSet(String key, long expect, long update) {
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        return atomic.compareAndSet(expect, update);
    }

    @Override
    public long get(String key) {
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        Long value = atomic.get();
        return value == null ? 0 : value;
    }

    @Override
    public void expire(String key, long ttl) {
        RAtomicLong atomic = redissonClient.getAtomicLong(key);
        atomic.expire(ttl, TimeUnit.SECONDS);
    }

    @Override
    public void delete(String key) {
        redissonClient.getAtomicLong(key).delete();
    }

    @Override
    public boolean exists(String key) {
        return redissonClient.getAtomicLong(key).isExists();
    }
}

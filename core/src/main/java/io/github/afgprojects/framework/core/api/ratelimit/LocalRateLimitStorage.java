package io.github.afgprojects.framework.core.api.ratelimit;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 本地内存限流存储实现
 * <p>
 * 使用 Caffeine 缓存实现本地限流，适用于单机部署或降级场景。
 * 滑动窗口使用环形缓冲区实现，避免 CopyOnWriteArrayList 的性能问题。
 * </p>
 */
public class LocalRateLimitStorage implements RateLimitStorage {

    private static final int DEFAULT_CACHE_SIZE = 10000;
    private static final int DEFAULT_EXPIRE_SECONDS = 3600;

    private final Cache<String, RateLimitCounter> counterCache;

    /**
     * 默认构造函数
     */
    public LocalRateLimitStorage() {
        this(DEFAULT_CACHE_SIZE, DEFAULT_EXPIRE_SECONDS);
    }

    /**
     * 构造函数
     *
     * @param cacheSize        缓存大小
     * @param expireAfterSeconds 过期时间（秒）
     */
    public LocalRateLimitStorage(int cacheSize, int expireAfterSeconds) {
        this.counterCache = Caffeine.newBuilder()
                .maximumSize(cacheSize)
                .expireAfterWrite(Duration.ofSeconds(expireAfterSeconds))
                .build();
    }

    @Override
    public String getStorageType() {
        return "local";
    }

    @Override
    public RateLimitResult tryAcquireTokenBucket(String key, long rate, long burst) {
        RateLimitCounter counter = counterCache.get(key, k -> {
            RateLimitCounter newCounter = new RateLimitCounter();
            newCounter.tokens = burst;
            return newCounter;
        });

        long now = System.currentTimeMillis();

        synchronized (counter) {
            long elapsed = now - counter.lastRefillTime;
            long tokensToAdd = (elapsed * rate) / 1000;

            counter.tokens = Math.min(burst, counter.tokens + tokensToAdd);
            counter.lastRefillTime = now;

            if (counter.tokens >= 1) {
                counter.tokens--;
                return RateLimitResult.allowed((long) counter.tokens, burst, now + 1000);
            } else {
                long retryAfter = (long) Math.ceil(1000.0 / rate);
                return RateLimitResult.rejected(burst, now + retryAfter, retryAfter);
            }
        }
    }

    @Override
    public RateLimitResult tryAcquireSlidingWindow(String key, long rate, long windowSize) {
        RateLimitCounter counter = counterCache.get(key, k -> new RateLimitCounter());

        long now = System.currentTimeMillis();
        long windowSizeMs = windowSize * 1000;
        long windowStart = now - windowSizeMs;

        synchronized (counter) {
            // 使用滑动计数器算法（环形缓冲区的简化版本）
            // 计算当前窗口内的请求数
            long currentCount = counter.count.get();
            long windowStartTime = counter.windowStartTime.get();

            // 如果窗口已过期，重置计数器
            if (windowStartTime < windowStart) {
                counter.count.set(1);
                counter.windowStartTime.set(now);
                return RateLimitResult.allowed(rate - 1, rate, now + windowSizeMs);
            }

            // 检查是否超过限制
            if (currentCount < rate) {
                counter.count.incrementAndGet();
                return RateLimitResult.allowed(rate - currentCount - 1, rate, now + windowSizeMs);
            } else {
                // 计算重试时间：窗口结束时间 - 当前时间
                long retryAfter = windowStartTime + windowSizeMs - now;
                return RateLimitResult.rejected(rate, now + windowSizeMs, Math.max(1, retryAfter));
            }
        }
    }

    @Override
    public long increment(String key, long delta, long ttl) {
        RateLimitCounter counter = counterCache.get(key, k -> new RateLimitCounter());
        synchronized (counter) {
            counter.tokens += delta;
            return (long) counter.tokens;
        }
    }

    @Override
    public boolean compareAndSet(String key, long expect, long update) {
        RateLimitCounter counter = counterCache.getIfPresent(key);
        if (counter == null) {
            return false;
        }
        synchronized (counter) {
            if ((long) counter.tokens == expect) {
                counter.tokens = update;
                return true;
            }
            return false;
        }
    }

    @Override
    public long get(String key) {
        RateLimitCounter counter = counterCache.getIfPresent(key);
        return counter == null ? 0 : (long) counter.tokens;
    }

    @Override
    public void expire(String key, long ttl) {
        // Caffeine 自动过期，此方法为空实现
    }

    @Override
    public void delete(String key) {
        counterCache.invalidate(key);
    }

    @Override
    public boolean exists(String key) {
        return counterCache.getIfPresent(key) != null;
    }

    /**
     * 本地限流计数器
     * <p>
     * 使用滑动计数器算法，避免 CopyOnWriteArrayList 的性能问题。
     * </p>
     */
    private static class RateLimitCounter {
        volatile double tokens = 0;
        volatile long lastRefillTime = System.currentTimeMillis();
        // 滑动窗口计数器
        final AtomicLong count = new AtomicLong(0);
        final AtomicLong windowStartTime = new AtomicLong(System.currentTimeMillis());
    }
}

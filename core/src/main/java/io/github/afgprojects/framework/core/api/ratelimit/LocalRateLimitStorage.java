package io.github.afgprojects.framework.core.api.ratelimit;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * 本地内存限流存储实现
 * <p>
 * 使用 Caffeine 缓存实现本地限流，适用于单机部署或降级场景。
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
            // 清理过期的请求
            counter.requests.removeIf(timestamp -> timestamp < windowStart);

            if (counter.requests.size() < rate) {
                counter.requests.add(now);
                return RateLimitResult.allowed(rate - counter.requests.size() - 1, rate, now + windowSizeMs);
            } else {
                long oldestRequest = counter.requests.isEmpty() ? now : counter.requests.get(0);
                long retryAfter = oldestRequest + windowSizeMs - now;
                return RateLimitResult.rejected(rate, now + windowSizeMs, retryAfter);
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
     */
    private static class RateLimitCounter {
        volatile double tokens = 0;
        volatile long lastRefillTime = System.currentTimeMillis();
        final List<Long> requests = new CopyOnWriteArrayList<>();
    }
}

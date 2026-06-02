package io.github.afgprojects.framework.ai.performance;

import io.github.afgprojects.framework.ai.core.api.performance.RateLimiter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认速率限制器实现
 *
 * <p>基于令牌桶算法的速率限制器，适用于：
 * <ul>
 *   <li>开发测试环境</li>
 *   <li>单机部署</li>
 *   <li>轻量级限流需求</li>
 * </ul>
 *
 * <p>生产环境建议使用 Redis + Lua 脚本或 Guava RateLimiter 实现。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultRateLimiter implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(DefaultRateLimiter.class);

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, RateLimitConfig> configs = new ConcurrentHashMap<>();

    private final long defaultPermits;
    private final Duration defaultWindow;

    /**
     * 创建默认速率限制器
     *
     * @param defaultPermits 默认许可数
     * @param defaultWindow  默认时间窗口
     */
    public DefaultRateLimiter(long defaultPermits, Duration defaultWindow) {
        this.defaultPermits = defaultPermits;
        this.defaultWindow = defaultWindow;
    }

    /**
     * 创建默认速率限制器（默认 100 请求/秒）
     */
    public DefaultRateLimiter() {
        this(100, Duration.ofSeconds(1));
    }

    @Override
    public boolean tryAcquire(@NonNull String key) {
        return tryAcquire(key, 1);
    }

    @Override
    public boolean tryAcquire(@NonNull String key, int permits) {
        TokenBucket bucket = getOrCreateBucket(key);
        return bucket.tryAcquire(permits);
    }

    @Override
    public void acquire(@NonNull String key) throws InterruptedException {
        acquire(key, 1, Duration.ofMillis(Long.MAX_VALUE));
    }

    @Override
    public boolean tryAcquire(@NonNull String key, @NonNull Duration timeout) throws InterruptedException {
        return acquire(key, 1, timeout);
    }

    private boolean acquire(String key, int permits, Duration timeout) throws InterruptedException {
        TokenBucket bucket = getOrCreateBucket(key);
        return bucket.tryAcquire(permits, timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    @NonNull
    public RateLimitStatus getStatus(@NonNull String key) {
        TokenBucket bucket = buckets.get(key);
        if (bucket == null) {
            return new DefaultRateLimitStatus(false, defaultPermits, Duration.ZERO, 0, System.currentTimeMillis() + defaultWindow.toMillis());
        }
        return bucket.getStatus();
    }

    @Override
    public void reset(@NonNull String key) {
        buckets.remove(key);
        log.debug("Reset rate limit for key: {}", key);
    }

    @Override
    @NonNull
    public List<String> getKeys() {
        return new ArrayList<>(buckets.keySet());
    }

    @Override
    public void setConfig(@NonNull String key, @NonNull RateLimitConfig config) {
        configs.put(key, config);
        // 重新创建桶
        buckets.remove(key);
        log.info("Set rate limit config for key {}: {} permits per {}", key, config.getPermits(), config.getWindow());
    }

    @Override
    @Nullable
    public RateLimitConfig getConfig(@NonNull String key) {
        return configs.get(key);
    }

    private TokenBucket getOrCreateBucket(String key) {
        return buckets.computeIfAbsent(key, k -> {
            RateLimitConfig config = configs.get(key);
            long permits = config != null ? config.getPermits() : defaultPermits;
            Duration window = config != null ? config.getWindow() : defaultWindow;
            return new TokenBucket(permits, window);
        });
    }

    /**
     * 令牌桶
     */
    private static class TokenBucket {
        private final long capacity;
        private final double refillRate; // 每毫秒补充的令牌数
        private final Duration window;

        private volatile double tokens;
        private volatile long lastRefillTime;
        private final ReentrantLock lock = new ReentrantLock();

        TokenBucket(long capacity, Duration window) {
            this.capacity = capacity;
            this.window = window;
            this.refillRate = capacity / (double) window.toMillis();
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        boolean tryAcquire(int permits) {
            lock.lock();
            try {
                refill();

                if (tokens >= permits) {
                    tokens -= permits;
                    return true;
                }
                return false;
            } finally {
                lock.unlock();
            }
        }

        boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
            long startTime = System.currentTimeMillis();
            long timeoutMs = unit.toMillis(timeout);

            while (true) {
                if (tryAcquire(permits)) {
                    return true;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed >= timeoutMs) {
                    return false;
                }

                // 计算等待时间
                long waitTime = Math.min(timeoutMs - elapsed, calculateWaitTime(permits));
                if (waitTime > 0) {
                    Thread.sleep(Math.min(waitTime, 100));
                }
            }
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;

            if (elapsed > 0) {
                double newTokens = elapsed * refillRate;
                tokens = Math.min(capacity, tokens + newTokens);
                lastRefillTime = now;
            }
        }

        private long calculateWaitTime(int permits) {
            double needed = permits - tokens;
            if (needed <= 0) return 0;
            return (long) (needed / refillRate);
        }

        RateLimitStatus getStatus() {
            lock.lock();
            try {
                refill();
                long usedPermits = (long) (capacity - tokens);
                long resetTime = System.currentTimeMillis() + window.toMillis();

                return new DefaultRateLimitStatus(
                        tokens < 1,
                        (long) tokens,
                        Duration.ofMillis(calculateWaitTime(1)),
                        usedPermits,
                        resetTime
                );
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 默认速率限制状态
     */
    private static class DefaultRateLimitStatus implements RateLimitStatus {
        private final boolean limited;
        private final long availablePermits;
        private final Duration waitTime;
        private final long usedPermits;
        private final long resetTime;

        DefaultRateLimitStatus(boolean limited, long availablePermits, Duration waitTime, long usedPermits, long resetTime) {
            this.limited = limited;
            this.availablePermits = availablePermits;
            this.waitTime = waitTime;
            this.usedPermits = usedPermits;
            this.resetTime = resetTime;
        }

        @Override
        public boolean isLimited() { return limited; }

        @Override
        public long getAvailablePermits() { return availablePermits; }

        @Override
        @NonNull
        public Duration getWaitTime() { return waitTime; }

        @Override
        public long getUsedPermits() { return usedPermits; }

        @Override
        public long getResetTime() { return resetTime; }
    }
}
package io.github.afgprojects.framework.core.api.ratelimit;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 限流存储降级实现
 * <p>
 * 当没有 Redis 等限流后端时，提供本地降级。
 * 所有限流请求总是放行，计数操作返回 0。
 */
public class NoOpRateLimitStorage implements RateLimitStorage {

    @Override
    public String getStorageType() {
        return "noop";
    }

    @Override
    public RateLimitResult tryAcquireTokenBucket(@NonNull String key, long rate, long burst) {
        return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE, 0);
    }

    @Override
    public RateLimitResult tryAcquireSlidingWindow(@NonNull String key, long rate, long windowSize) {
        return RateLimitResult.allowed(Long.MAX_VALUE, Long.MAX_VALUE, 0);
    }

    @Override
    public long increment(@NonNull String key, long delta, long ttl) {
        return 0L;
    }

    @Override
    public boolean compareAndSet(@NonNull String key, long expect, long update) {
        return false;
    }

    @Override
    public long get(@NonNull String key) {
        return 0;
    }

    @Override
    public void expire(@NonNull String key, long ttl) {
        // no-op
    }

    @Override
    public void delete(@NonNull String key) {
        // no-op
    }

    @Override
    public boolean exists(@NonNull String key) {
        return false;
    }
}

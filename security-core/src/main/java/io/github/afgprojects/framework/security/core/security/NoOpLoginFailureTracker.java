package io.github.afgprojects.framework.security.core.security;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * NoOp 登录失败追踪降级实现。
 * <p>
 * 总是记录 0 次失败（不追踪登录失败）。
 *
 * @since 1.0.0
 */
public class NoOpLoginFailureTracker implements LoginFailureTracker {

    @Override
    public void recordFailure(String userId, @Nullable String tenantId, String ip) {
        // no-op
    }

    @Override
    public int getFailureCount(String userId, @Nullable String tenantId) {
        return 0;
    }

    @Override
    public boolean isLocked(String userId, @Nullable String tenantId) {
        return false;
    }

    @Override
    public @Nullable LocalDateTime getLockedUntil(String userId, @Nullable String tenantId) {
        return null;
    }

    @Override
    public void unlock(String userId, @Nullable String tenantId) {
        // no-op
    }

    @Override
    public void reset(String userId, @Nullable String tenantId) {
        // no-op
    }
}

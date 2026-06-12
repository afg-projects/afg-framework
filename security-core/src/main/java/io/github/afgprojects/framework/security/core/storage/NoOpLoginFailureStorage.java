package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * NoOp 登录失败存储降级实现。
 * <p>
 * 总是记录 0 次失败（不追踪登录失败）。
 *
 * @since 1.0.0
 */
public class NoOpLoginFailureStorage implements AfgLoginFailureStorage {

    @Override
    public void recordFailure(@NonNull String userId, @NonNull String username,
            @Nullable String tenantId, @Nullable String ip) {
        // no-op
    }

    @Override
    public int getFailureCount(@NonNull String userId) {
        return 0;
    }

    @Override
    public boolean isLocked(@NonNull String userId) {
        return false;
    }

    @Override
    public @Nullable Instant getLockedUntil(@NonNull String userId) {
        return null;
    }

    @Override
    public void unlock(@NonNull String userId) {
        // no-op
    }

    @Override
    public void reset(@NonNull String userId) {
        // no-op
    }
}

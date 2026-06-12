package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;

import java.time.Duration;

/**
 * NoOp Token 黑名单降级实现。
 * <p>
 * 总是返回 false（不在黑名单），加入黑名单为空操作。
 *
 * @since 1.0.0
 */
public class NoOpTokenBlacklist implements AfgTokenBlacklist {

    @Override
    public void addToBlacklist(@NonNull String tokenHash, @NonNull String userId,
            @NonNull String reason, @NonNull Duration ttl) {
        // no-op
    }

    @Override
    public boolean isBlacklisted(@NonNull String tokenHash) {
        return false;
    }

    @Override
    public void blacklistAllUserTokens(@NonNull String userId, @NonNull Duration ttl) {
        // no-op
    }
}

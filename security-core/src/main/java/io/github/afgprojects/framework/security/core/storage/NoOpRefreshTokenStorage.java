package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Optional;

/**
 * NoOp 刷新令牌存储降级实现。
 * <p>
 * 存储操作为空操作，查询返回空/不存在。
 *
 * @since 1.0.0
 */
public class NoOpRefreshTokenStorage implements AfgRefreshTokenStorage {

    @Override
    public void save(@NonNull String tokenId, @NonNull String tokenHash, @NonNull String userId,
            @Nullable String tenantId, @Nullable String clientId, @Nullable String deviceId,
            @NonNull Instant expiresAt) {
        // no-op
    }

    @Override
    public @NonNull Optional<RefreshTokenInfo> findByTokenHash(@NonNull String tokenHash) {
        return Optional.empty();
    }

    @Override
    public @NonNull Optional<RefreshTokenInfo> findByTokenId(@NonNull String tokenId) {
        return Optional.empty();
    }

    @Override
    public void delete(@NonNull String tokenId) {
        // no-op
    }

    @Override
    public void deleteByUserId(@NonNull String userId) {
        // no-op
    }

    @Override
    public int deleteExpired() {
        return 0;
    }
}

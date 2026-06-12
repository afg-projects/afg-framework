package io.github.afgprojects.framework.security.core.login;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * NoOp 令牌服务降级实现。
 * <p>
 * Token 操作返回空/无效结果，验证总是返回 false。
 *
 * @since 1.0.0
 */
public class NoOpTokenService implements TokenService {

    @Override
    public @NonNull String generateAccessToken(@NonNull String userId, @NonNull String username,
            @NonNull Set<String> roles, @NonNull Set<String> permissions, @Nullable String tenantId) {
        return "noop-access-token";
    }

    @Override
    public @NonNull String generateRefreshToken(@NonNull String userId, @Nullable String tenantId) {
        return "noop-refresh-token";
    }

    @Override
    public boolean validateAccessToken(@NonNull String token) {
        return false;
    }

    @Override
    public boolean validateRefreshToken(@NonNull String refreshToken) {
        return false;
    }

    @Override
    public @Nullable String extractUserId(@NonNull String token) {
        return null;
    }

    @Override
    public @Nullable String extractUsername(@NonNull String token) {
        return null;
    }

    @Override
    public @NonNull Set<String> extractRoles(@NonNull String token) {
        return Set.of();
    }

    @Override
    public @NonNull Set<String> extractPermissions(@NonNull String token) {
        return Set.of();
    }

    @Override
    public @Nullable String extractTenantId(@NonNull String token) {
        return null;
    }

    @Override
    public void invalidateToken(@NonNull String token) {
        // no-op
    }

    @Override
    public void invalidateAllTokens(@NonNull String userId) {
        // no-op
    }

    @Override
    public long getAccessTokenTtl() {
        return 0;
    }

    @Override
    public long getRefreshTokenTtl() {
        return 0;
    }
}

package io.github.afgprojects.framework.security.core.storage;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

/**
 * NoOp Token 黑名单降级实现。
 * <p>
 * 总是返回 false（不在黑名单），加入黑名单为空操作。
 *
 * <p><b>安全警告：</b>此实现下 Token 吊销完全无效，已注销或已吊销的 Token
 * 仍可正常使用。生产环境必须提供 {@link AfgTokenBlacklist} 的真实实现
 * （如 JDBC、Redis），否则 Token 安全机制形同虚设。
 *
 * @since 1.0.0
 */
@Slf4j
public class NoOpTokenBlacklist implements AfgTokenBlacklist {

    private static final String WARNING_MESSAGE =
            "Token blacklisting is DISABLED! Revoked tokens will still be accepted. "
            + "This is a critical security risk in production. "
            + "Implement an AfgTokenBlacklist bean (e.g., JdbcTokenBlacklist) to enable token revocation.";

    private static final AtomicBoolean warned = new AtomicBoolean(false);

    @Override
    public void addToBlacklist(@NonNull String tokenHash, @NonNull String userId,
            @NonNull String reason, @NonNull Duration ttl) {
        if (warned.compareAndSet(false, true)) {
            log.warn(WARNING_MESSAGE);
        }
    }

    @Override
    public boolean isBlacklisted(@NonNull String tokenHash) {
        return false;
    }

    @Override
    public void blacklistAllUserTokens(@NonNull String userId, @NonNull Duration ttl) {
        if (warned.compareAndSet(false, true)) {
            log.warn(WARNING_MESSAGE);
        }
    }
}

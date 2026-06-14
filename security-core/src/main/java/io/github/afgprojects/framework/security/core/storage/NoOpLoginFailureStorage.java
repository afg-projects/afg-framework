package io.github.afgprojects.framework.security.core.storage;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * NoOp 登录失败存储降级实现。
 * <p>
 * 总是记录 0 次失败（不追踪登录失败）。
 *
 * <p><b>安全警告：</b>此实现下暴力破解保护完全禁用，攻击者可无限次尝试登录。
 * 生产环境必须提供 {@link AfgLoginFailureStorage} 的真实实现
 * （如 JdbcLoginFailureStorage），否则系统面临暴力破解风险。
 *
 * @since 1.0.0
 */
@Slf4j
public class NoOpLoginFailureStorage implements AfgLoginFailureStorage {

    private static final String WARNING_MESSAGE =
            "Login failure tracking is DISABLED! Brute-force protection is not active. "
            + "This is a critical security risk in production. "
            + "Implement an AfgLoginFailureStorage bean (e.g., JdbcLoginFailureStorage) to enable brute-force protection.";

    private static final AtomicBoolean warned = new AtomicBoolean(false);

    @Override
    public void recordFailure(@NonNull String userId, @NonNull String username,
            @Nullable String tenantId, @Nullable String ip) {
        if (warned.compareAndSet(false, true)) {
            log.warn(WARNING_MESSAGE);
        }
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

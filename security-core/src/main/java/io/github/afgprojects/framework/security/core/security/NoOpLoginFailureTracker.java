package io.github.afgprojects.framework.security.core.security;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/**
 * NoOp 登录失败追踪降级实现。
 * <p>
 * 总是记录 0 次失败（不追踪登录失败）。
 *
 * <p><b>安全警告：</b>此实现下暴力破解保护完全禁用，攻击者可无限次尝试登录。
 * 生产环境必须提供 {@link LoginFailureTracker} 的真实实现
 * （如 DefaultLoginFailureTracker + AfgLoginFailureStorage），否则系统面临暴力破解风险。
 *
 * @since 1.0.0
 */
@Slf4j
public class NoOpLoginFailureTracker implements LoginFailureTracker {

    private static final String WARNING_MESSAGE =
            "Login failure tracking is DISABLED! Brute-force protection is not active. "
            + "This is a critical security risk in production. "
            + "Implement a LoginFailureTracker bean (e.g., DefaultLoginFailureTracker) to enable brute-force protection.";

    private static final AtomicBoolean warned = new AtomicBoolean(false);

    @Override
    public void recordFailure(String userId, @Nullable String tenantId, String ip) {
        if (warned.compareAndSet(false, true)) {
            log.warn(WARNING_MESSAGE);
        }
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

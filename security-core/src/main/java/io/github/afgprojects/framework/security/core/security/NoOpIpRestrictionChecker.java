package io.github.afgprojects.framework.security.core.security;

import org.jspecify.annotations.Nullable;

/**
 * NoOp IP 限制检查降级实现。
 * <p>
 * 总是允许访问（不限制 IP）。
 *
 * @since 1.0.0
 */
public class NoOpIpRestrictionChecker implements IpRestrictionChecker {

    @Override
    public boolean isAllowed(String ip, @Nullable String userId, @Nullable String tenantId) {
        return true;
    }

    @Override
    public boolean isBlacklisted(String ip) {
        return false;
    }

    @Override
    public boolean isWhitelisted(String ip) {
        return false;
    }
}

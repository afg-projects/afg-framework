package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.entity.AuthTokenBlacklist;
import io.github.afgprojects.framework.security.core.storage.AfgTokenBlacklist;

import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.time.LocalDateTime;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

/**
 * 基于 DataManager 的 Token 黑名单存储。
 *
 * <p>将 Token 黑名单持久化到关系型数据库，支持：
 * <ul>
 *   <li>Token 加入黑名单</li>
 *   <li>检查 Token 是否在黑名单中</li>
 *   <li>将用户所有 Token 加入黑名单</li>
 *   <li>自动清理过期记录</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcTokenBlacklist implements AfgTokenBlacklist {

    private final DataManager dataManager;

    /**
     * 构造函数。
     *
     * @param dataManager 数据管理器
     */
    public JdbcTokenBlacklist(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @Override
    public void addToBlacklist(
            @NonNull String tokenHash,
            @NonNull String userId,
            @NonNull String reason,
            @NonNull Duration ttl
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(ttl);

        var existing = dataManager.findOneByField(AuthTokenBlacklist.class,
                AuthTokenBlacklist::getTokenHash, tokenHash);

        AuthTokenBlacklist entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setUserId(userId);
            entity.setReason(reason);
            entity.setExpiresAt(expiresAt);
            entity.setCreatedAt(now);
        } else {
            entity = new AuthTokenBlacklist();
            entity.setTokenHash(tokenHash);
            entity.setUserId(userId);
            entity.setReason(reason);
            entity.setExpiresAt(expiresAt);
            entity.setCreatedAt(now);
        }
        dataManager.save(AuthTokenBlacklist.class, entity);
        log.debug("Added token to blacklist: tokenHash={}, userId={}, reason={}", tokenHash, userId, reason);
    }

    @Override
    public boolean isBlacklisted(@NonNull String tokenHash) {
        deleteExpired();
        return dataManager.existsByCondition(AuthTokenBlacklist.class,
                builder(AuthTokenBlacklist.class)
                        .eq(AuthTokenBlacklist::getTokenHash, tokenHash)
                        .build());
    }

    @Override
    public void blacklistAllUserTokens(@NonNull String userId, @NonNull Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plus(ttl);
        String reason = "user_logout_all";

        String userBlacklistTokenHash = "user_all:" + userId;

        var existing = dataManager.findOneByField(AuthTokenBlacklist.class,
                AuthTokenBlacklist::getTokenHash, userBlacklistTokenHash);

        AuthTokenBlacklist entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setExpiresAt(expiresAt);
            entity.setCreatedAt(now);
        } else {
            entity = new AuthTokenBlacklist();
            entity.setTokenHash(userBlacklistTokenHash);
            entity.setUserId(userId);
            entity.setReason(reason);
            entity.setExpiresAt(expiresAt);
            entity.setCreatedAt(now);
        }
        dataManager.save(AuthTokenBlacklist.class, entity);
        log.info("Blacklisted all tokens for user: userId={}, ttl={}", userId, ttl);
    }

    /**
     * 删除过期的黑名单记录。
     *
     * @return 删除的记录数
     */
    public int deleteExpired() {
        LocalDateTime now = LocalDateTime.now();
        var entities = dataManager.findList(AuthTokenBlacklist.class,
                builder(AuthTokenBlacklist.class)
                        .lt(AuthTokenBlacklist::getExpiresAt, now)
                        .build());

        for (var entity : entities) {
            dataManager.deleteById(AuthTokenBlacklist.class, entity.getId());
        }

        int count = entities.size();
        if (count > 0) {
            log.debug("Deleted expired blacklist records: count={}", count);
        }
        return count;
    }

    /**
     * 检查用户是否被全局拉黑（所有 Token 都在黑名单中）。
     *
     * @param userId 用户 ID
     * @return 如果用户被全局拉黑则返回 true
     */
    public boolean isUserBlacklisted(@NonNull String userId) {
        String userBlacklistTokenHash = "user_all:" + userId;
        return dataManager.existsByCondition(AuthTokenBlacklist.class,
                builder(AuthTokenBlacklist.class)
                        .eq(AuthTokenBlacklist::getUserId, userId)
                        .eq(AuthTokenBlacklist::getTokenHash, userBlacklistTokenHash)
                        .gt(AuthTokenBlacklist::getExpiresAt, LocalDateTime.now())
                        .build());
    }

    /**
     * 检查 Token 是否在黑名单中（包括用户级别的黑名单检查）。
     *
     * @param tokenHash Token 的哈希值
     * @param userId    用户 ID
     * @return 如果在黑名单中则返回 true
     */
    public boolean isBlacklistedWithUserCheck(@NonNull String tokenHash, @NonNull String userId) {
        if (isBlacklisted(tokenHash)) {
            return true;
        }
        return isUserBlacklisted(userId);
    }
}

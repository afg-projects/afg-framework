package io.github.afgprojects.framework.security.auth.storage;

import lombok.extern.slf4j.Slf4j;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.security.auth.entity.AuthLoginFailure;
import io.github.afgprojects.framework.security.core.storage.AfgLoginFailureStorage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Optional;

import static io.github.afgprojects.framework.data.core.condition.Conditions.*;

/**
 * 基于 DataManager 的登录失败存储。
 *
 * <p>将登录失败记录持久化到关系型数据库，支持：
 * <ul>
 *   <li>记录登录失败次数</li>
 *   <li>账户锁定管理</li>
 *   <li>失败记录重置</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Slf4j
public class JdbcLoginFailureStorage implements AfgLoginFailureStorage {

    /** 默认锁定阈值 */
    private static final int DEFAULT_LOCK_THRESHOLD = 5;

    /** 默认锁定时长（分钟） */
    private static final int DEFAULT_LOCK_DURATION_MINUTES = 30;

    private final DataManager dataManager;
    private final int lockThreshold;
    private final int lockDurationMinutes;

    /**
     * 构造函数，使用默认锁定策略。
     *
     * @param dataManager 数据管理器
     */
    public JdbcLoginFailureStorage(@NonNull DataManager dataManager) {
        this(dataManager, DEFAULT_LOCK_THRESHOLD, DEFAULT_LOCK_DURATION_MINUTES);
    }

    /**
     * 构造函数，使用自定义锁定策略。
     *
     * @param dataManager          数据管理器
     * @param lockThreshold        锁定阈值（失败多少次后锁定）
     * @param lockDurationMinutes  锁定时长（分钟）
     */
    public JdbcLoginFailureStorage(
            @NonNull DataManager dataManager,
            int lockThreshold,
            int lockDurationMinutes
    ) {
        this.dataManager = dataManager;
        this.lockThreshold = lockThreshold;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Override
    public void recordFailure(
            @NonNull String userId,
            @NonNull String username,
            @Nullable String tenantId,
            @Nullable String ip
    ) {
        LocalDateTime now = LocalDateTime.now();

        var existing = dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId);

        AuthLoginFailure entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setFailureCount(entity.getFailureCount() + 1);
            entity.setLastFailureIp(ip);
            entity.setLastFailureTime(now);
            entity.setUpdatedAt(now);
        } else {
            entity = new AuthLoginFailure();
            entity.setUserId(userId);
            entity.setUsername(username);
            entity.setTenantId(tenantId);
            entity.setFailureCount(1);
            entity.setLastFailureIp(ip);
            entity.setLastFailureTime(now);
            entity.setCreatedAt(now);
        }
        dataManager.save(AuthLoginFailure.class, entity);

        // 检查是否需要锁定
        int failureCount = entity.getFailureCount();
        if (failureCount >= lockThreshold) {
            LocalDateTime lockedUntil = now.plusMinutes(lockDurationMinutes);
            entity.setLockedUntil(lockedUntil);
            dataManager.save(AuthLoginFailure.class, entity);
            log.info("Account locked: userId={}, failureCount={}, lockedUntil={}", userId, failureCount, lockedUntil);
        }

        log.debug("Recorded login failure: userId={}, username={}, failureCount={}", userId, username, failureCount);
    }

    @Override
    public int getFailureCount(@NonNull String userId) {
        return dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId)
                .map(AuthLoginFailure::getFailureCount)
                .orElse(0);
    }

    @Override
    public boolean isLocked(@NonNull String userId) {
        return dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId)
                .map(entity -> {
                    LocalDateTime lockedUntil = entity.getLockedUntil();
                    return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
                })
                .orElse(false);
    }

    @Override
    @Nullable
    public LocalDateTime getLockedUntil(@NonNull String userId) {
        return dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId)
                .map(AuthLoginFailure::getLockedUntil)
                .filter(lockedUntil -> lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    @Override
    public void unlock(@NonNull String userId) {
        dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId)
                .ifPresent(entity -> {
                    entity.setFailureCount(0);
                    entity.setLockedUntil(null);
                    dataManager.save(AuthLoginFailure.class, entity);
                    log.info("Unlocked account: userId={}", userId);
                });
    }

    @Override
    public void reset(@NonNull String userId) {
        dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId)
                .ifPresent(entity -> {
                    entity.setFailureCount(0);
                    entity.setLockedUntil(null);
                    dataManager.save(AuthLoginFailure.class, entity);
                    log.debug("Reset failure count: userId={}", userId);
                });
    }

    /**
     * 获取失败记录详情。
     *
     * @param userId 用户 ID
     * @return 失败记录，如果不存在则返回空
     */
    @NonNull
    public Optional<FailureRecord> getFailureRecord(@NonNull String userId) {
        return dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId)
                .map(entity -> new FailureRecord(
                        entity.getFailureCount(),
                        entity.getLockedUntil(),
                        entity.getLastFailureIp()
                ));
    }

    /**
     * 删除用户的失败记录。
     *
     * @param userId 用户 ID
     */
    public void delete(@NonNull String userId) {
        dataManager.findOneByField(AuthLoginFailure.class,
                AuthLoginFailure::getUserId, userId)
                .ifPresent(entity -> {
                    dataManager.deleteById(AuthLoginFailure.class, entity.getId());
                    log.debug("Deleted failure record: userId={}", userId);
                });
    }

    /**
     * 清理过期的锁定记录。
     *
     * <p>将已过期的锁定状态清除，但保留失败记录。
     *
     * @return 更新的记录数
     */
    public int clearExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        var expiredRecords = dataManager.entity(AuthLoginFailure.class)
                .query()
                .where(builder(AuthLoginFailure.class)
                        .isNotNull(AuthLoginFailure::getLockedUntil)
                        .lt(AuthLoginFailure::getLockedUntil, now)
                        .build())
                .list();

        for (AuthLoginFailure entity : expiredRecords) {
            entity.setLockedUntil(null);
            dataManager.save(AuthLoginFailure.class, entity);
        }

        if (!expiredRecords.isEmpty()) {
            log.info("Cleared expired locks: count={}", expiredRecords.size());
        }
        return expiredRecords.size();
    }
}
package io.github.afgprojects.framework.security.auth.security;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.afgprojects.framework.security.core.security.LoginFailureTracker;

/**
 * 基于 Caffeine 缓存的登录失败追踪器默认实现。
 *
 * <p>使用 Caffeine 本地缓存存储登录失败记录，支持配置最大失败次数和锁定时间。
 *
 * <p>特性：
 * <ul>
 *   <li>基于内存存储，适用于单机部署场景</li>
 *   <li>支持多租户隔离</li>
 *   <li>自动过期清理</li>
 *   <li>线程安全</li>
 * </ul>
 *
 * @since 1.0.0
 */
public class DefaultLoginFailureTracker implements LoginFailureTracker {

    /**
     * 失败记录缓存
     */
    private final Cache<String, LoginFailureRecord> failureCache;

    /**
     * 最大失败次数
     */
    private final int maxFailures;

    /**
     * 锁定时长
     */
    private final Duration lockDuration;

    /**
     * 创建默认的登录失败追踪器。
     *
     * @param maxFailures 最大失败次数，达到此数值后账户将被锁定
     * @param lockDuration 锁定时长，锁定期间禁止登录
     */
    public DefaultLoginFailureTracker(int maxFailures, Duration lockDuration) {
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;

        // 缓存配置：锁定时长的 2 倍作为过期时间，确保锁定状态不会过早被清理
        Duration expireDuration = lockDuration.multipliedBy(2);

        this.failureCache = Caffeine.newBuilder()
                .expireAfterWrite(expireDuration.toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(10000)
                .build();
    }

    @Override
    public void recordFailure(String userId, @Nullable String tenantId, String ip) {
        String cacheKey = buildCacheKey(userId, tenantId);
        LoginFailureRecord record = failureCache.get(cacheKey, k -> new LoginFailureRecord());

        synchronized (record) {
            record.incrementFailure();
            record.setLastFailureTime(LocalDateTime.now());
            record.setLastFailureIp(ip);

            // 如果达到最大失败次数，设置锁定时间
            if (record.getFailureCount() >= maxFailures && record.getLockedUntil() == null) {
                record.setLockedUntil(LocalDateTime.now().plus(lockDuration));
            }
        }

        failureCache.put(cacheKey, record);
    }

    @Override
    public int getFailureCount(String userId, @Nullable String tenantId) {
        String cacheKey = buildCacheKey(userId, tenantId);
        LoginFailureRecord record = failureCache.getIfPresent(cacheKey);

        if (record == null) {
            return 0;
        }

        synchronized (record) {
            return record.getFailureCount();
        }
    }

    @Override
    public boolean isLocked(String userId, @Nullable String tenantId) {
        String cacheKey = buildCacheKey(userId, tenantId);
        LoginFailureRecord record = failureCache.getIfPresent(cacheKey);

        if (record == null) {
            return false;
        }

        synchronized (record) {
            LocalDateTime lockedUntil = record.getLockedUntil();
            if (lockedUntil == null) {
                return false;
            }

            // 检查锁定是否已过期
            if (LocalDateTime.now().isAfter(lockedUntil)) {
                // 锁定已过期，清除锁定状态
                record.setLockedUntil(null);
                return false;
            }

            return true;
        }
    }

    @Override
    @Nullable
    public LocalDateTime getLockedUntil(String userId, @Nullable String tenantId) {
        String cacheKey = buildCacheKey(userId, tenantId);
        LoginFailureRecord record = failureCache.getIfPresent(cacheKey);

        if (record == null) {
            return null;
        }

        synchronized (record) {
            LocalDateTime lockedUntil = record.getLockedUntil();
            if (lockedUntil == null) {
                return null;
            }

            // 检查锁定是否已过期
            if (LocalDateTime.now().isAfter(lockedUntil)) {
                // 锁定已过期，清除锁定状态
                record.setLockedUntil(null);
                return null;
            }

            return lockedUntil;
        }
    }

    @Override
    public void unlock(String userId, @Nullable String tenantId) {
        String cacheKey = buildCacheKey(userId, tenantId);
        LoginFailureRecord record = failureCache.getIfPresent(cacheKey);

        if (record != null) {
            synchronized (record) {
                record.setLockedUntil(null);
            }
        }
    }

    @Override
    public void reset(String userId, @Nullable String tenantId) {
        String cacheKey = buildCacheKey(userId, tenantId);
        failureCache.invalidate(cacheKey);
    }

    /**
     * 构建缓存 Key。
     *
     * @param userId 用户 ID
     * @param tenantId 租户 ID，可为 null
     * @return 缓存 Key
     */
    private String buildCacheKey(String userId, @Nullable String tenantId) {
        if (tenantId == null) {
            return userId;
        }
        return tenantId + ":" + userId;
    }

    /**
     * 登录失败记录。
     */
    private static class LoginFailureRecord {

        /**
         * 失败次数
         */
        private int failureCount = 0;

        /**
         * 最后失败时间
         */
        private LocalDateTime lastFailureTime;

        /**
         * 最后失败 IP
         */
        private String lastFailureIp;

        /**
         * 锁定截止时间
         */
        private LocalDateTime lockedUntil;

        public int getFailureCount() {
            return failureCount;
        }

        public void incrementFailure() {
            this.failureCount++;
        }

        public LocalDateTime getLastFailureTime() {
            return lastFailureTime;
        }

        public void setLastFailureTime(LocalDateTime lastFailureTime) {
            this.lastFailureTime = lastFailureTime;
        }

        public String getLastFailureIp() {
            return lastFailureIp;
        }

        public void setLastFailureIp(String lastFailureIp) {
            this.lastFailureIp = lastFailureIp;
        }

        public LocalDateTime getLockedUntil() {
            return lockedUntil;
        }

        public void setLockedUntil(LocalDateTime lockedUntil) {
            this.lockedUntil = lockedUntil;
        }
    }
}

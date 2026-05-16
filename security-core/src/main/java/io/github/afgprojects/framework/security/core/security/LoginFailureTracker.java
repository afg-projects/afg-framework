package io.github.afgprojects.framework.security.core.security;

import java.time.LocalDateTime;
import org.jspecify.annotations.Nullable;

/**
 * 登录失败追踪器接口。
 *
 * <p>用于追踪用户登录失败次数，实现账户锁定功能。
 *
 * <p>实现类可以基于内存、Redis 或数据库存储失败记录。
 *
 * @since 1.0.0
 */
public interface LoginFailureTracker {

    /**
     * 记录一次登录失败。
     *
     * <p>每次调用此方法会增加失败计数，并可能触发账户锁定。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @param ip 登录 IP 地址，永不为 null
     */
    void recordFailure(String userId, @Nullable String tenantId, String ip);

    /**
     * 获取登录失败次数。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 失败次数，如果没有记录则返回 0
     */
    int getFailureCount(String userId, @Nullable String tenantId);

    /**
     * 检查账户是否被锁定。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 如果账户被锁定则返回 true
     */
    boolean isLocked(String userId, @Nullable String tenantId);

    /**
     * 获取账户锁定截止时间。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 锁定截止时间，如果未锁定则返回 null
     */
    @Nullable
    LocalDateTime getLockedUntil(String userId, @Nullable String tenantId);

    /**
     * 手动解锁账户。
     *
     * <p>管理员可以使用此方法手动解锁被锁定的账户。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     */
    void unlock(String userId, @Nullable String tenantId);

    /**
     * 重置失败计数。
     *
     * <p>登录成功后应调用此方法清除失败记录。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     */
    void reset(String userId, @Nullable String tenantId);
}

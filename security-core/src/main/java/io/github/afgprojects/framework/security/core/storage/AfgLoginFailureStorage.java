package io.github.afgprojects.framework.security.core.storage;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 登录失败存储接口。
 *
 * <p>定义登录失败记录的存储、查询和管理操作。
 *
 * <p>实现类可以基于内存、Redis、数据库等存储介质。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>记录用户登录失败次数</li>
 *   <li>实现账户锁定策略</li>
 *   <li>防止暴力破解攻击</li>
 *   <li>安全审计和告警</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface AfgLoginFailureStorage {

    /**
     * 记录登录失败。
     *
     * <p>每次登录失败时调用此方法，失败计数器将递增。
     * 如果失败次数达到阈值，账户将被自动锁定。
     *
     * @param userId   用户 ID，永不为 null
     * @param username 用户名，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @param ip       登录 IP 地址，可选
     */
    void recordFailure(
            @NonNull String userId,
            @NonNull String username,
            @Nullable String tenantId,
            @Nullable String ip
    );

    /**
     * 获取失败次数。
     *
     * <p>返回当前累计的登录失败次数。
     * 如果用户没有失败记录，返回 0。
     *
     * @param userId 用户 ID，永不为 null
     * @return 登录失败次数，永不为负数
     */
    int getFailureCount(@NonNull String userId);

    /**
     * 检查是否锁定。
     *
     * <p>检查用户账户是否因登录失败次数过多而被锁定。
     *
     * @param userId 用户 ID，永不为 null
     * @return 如果账户被锁定则返回 true
     */
    boolean isLocked(@NonNull String userId);

    /**
     * 获取锁定截止时间。
     *
     * <p>如果账户被锁定，返回锁定的截止时间。
     * 锁定时间过后，账户将自动解锁。
     *
     * @param userId 用户 ID，永不为 null
     * @return 锁定截止时间，如果未锁定则返回 null
     */
    @Nullable
    LocalDateTime getLockedUntil(@NonNull String userId);

    /**
     * 解锁账户。
     *
     * <p>手动解锁被锁定的账户，同时清除失败记录。
     *
     * @param userId 用户 ID，永不为 null
     */
    void unlock(@NonNull String userId);

    /**
     * 重置失败次数。
     *
     * <p>登录成功后调用此方法，清除失败记录。
     *
     * @param userId 用户 ID，永不为 null
     */
    void reset(@NonNull String userId);

    /**
     * 登录失败记录。
     *
     * <p>包含失败次数、锁定截止时间和最后失败 IP。
     *
     * @param count       失败次数
     * @param lockedUntil 锁定截止时间，如果未锁定则为 null
     * @param lastIp      最后一次失败的 IP 地址，可能为 null
     */
    record FailureRecord(
            int count,
            @Nullable LocalDateTime lockedUntil,
            @Nullable String lastIp
    ) {
    }
}

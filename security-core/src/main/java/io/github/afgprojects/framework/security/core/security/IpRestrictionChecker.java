package io.github.afgprojects.framework.security.core.security;

import org.jspecify.annotations.Nullable;

/**
 * IP 限制检查器接口。
 *
 * <p>用于检查 IP 地址是否被允许访问系统。
 *
 * <p>支持黑名单和白名单两种模式：
 * <ul>
 *   <li>黑名单模式：禁止列表中的 IP 访问</li>
 *   <li>白名单模式：只允许列表中的 IP 访问</li>
 * </ul>
 *
 * <p>实现类可以基于内存、数据库或外部服务（如防火墙）进行 IP 检查。
 *
 * @since 1.0.0
 */
public interface IpRestrictionChecker {

    /**
     * 检查 IP 是否被允许访问。
     *
     * <p>综合考虑黑名单、白名单和用户/租户级别的 IP 限制。
     *
     * @param ip IP 地址，永不为 null
     * @param userId 用户 ID，可为 null（未登录时）
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 如果允许访问则返回 true
     */
    boolean isAllowed(String ip, @Nullable String userId, @Nullable String tenantId);

    /**
     * 检查 IP 是否在黑名单中。
     *
     * <p>黑名单中的 IP 将被禁止访问系统。
     *
     * @param ip IP 地址，永不为 null
     * @return 如果在黑名单中则返回 true
     */
    boolean isBlacklisted(String ip);

    /**
     * 检查 IP 是否在白名单中。
     *
     * <p>白名单中的 IP 通常可以绕过某些安全检查。
     *
     * @param ip IP 地址，永不为 null
     * @return 如果在白名单中则返回 true
     */
    boolean isWhitelisted(String ip);
}

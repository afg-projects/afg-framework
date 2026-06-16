package io.github.afgprojects.framework.security.core.login;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 令牌服务接口。
 *
 * <p>定义令牌生成、验证和管理的标准接口。
 *
 * <p>实现类可以基于 JWT、OAuth2 或其他令牌机制。
 *
 * @since 1.0.0
 */
public interface TokenService {

    /**
     * 生成访问令牌。
     *
     * @param userId 用户 ID，永不为 null
     * @param username 用户名，永不为 null
     * @param roles 角色集合，永不为 null
     * @param permissions 权限集合，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 访问令牌，永不为 null
     */
    @NonNull
    String generateAccessToken(
            @NonNull String userId,
            @NonNull String username,
            @NonNull Set<String> roles,
            @NonNull Set<String> permissions,
            @Nullable String tenantId);

    /**
     * 生成刷新令牌。
     *
     * @param userId 用户 ID，永不为 null
     * @param tenantId 租户 ID，单租户场景可为 null
     * @return 刷新令牌，永不为 null
     */
    @NonNull
    String generateRefreshToken(@NonNull String userId, @Nullable String tenantId);

    /**
     * 验证访问令牌。
     *
     * @param token 访问令牌，永不为 null
     * @return 如果令牌有效则返回 true，否则返回 false
     */
    boolean validateAccessToken(@NonNull String token);

    /**
     * 验证刷新令牌。
     *
     * @param refreshToken 刷新令牌，永不为 null
     * @return 如果令牌有效则返回 true，否则返回 false
     */
    boolean validateRefreshToken(@NonNull String refreshToken);

    /**
     * 从令牌中提取用户 ID。
     *
     * @param token 访问令牌，永不为 null
     * @return 用户 ID，如果令牌无效则返回 null
     */
    @Nullable
    String extractUserId(@NonNull String token);

    /**
     * 从令牌中提取用户名。
     *
     * @param token 访问令牌，永不为 null
     * @return 用户名，如果令牌无效则返回 null
     */
    @Nullable
    String extractUsername(@NonNull String token);

    /**
     * 从令牌中提取角色集合。
     *
     * @param token 访问令牌，永不为 null
     * @return 角色集合，如果令牌无效或无角色则返回空集合
     */
    @NonNull
    Set<String> extractRoles(@NonNull String token);

    /**
     * 从令牌中提取权限集合。
     *
     * @param token 访问令牌，永不为 null
     * @return 权限集合，如果令牌无效或无权限则返回空集合
     */
    @NonNull
    Set<String> extractPermissions(@NonNull String token);

    /**
     * 从令牌中提取租户 ID。
     *
     * @param token 访问令牌，永不为 null
     * @return 租户 ID，如果令牌无效或无租户信息则返回 null
     */
    @Nullable
    String extractTenantId(@NonNull String token);

    /**
     * 撤销令牌。
     *
     * <p>使令牌立即失效，后续验证将返回 false。
     *
     * @param token 访问令牌，永不为 null
     */
    void invalidateToken(@NonNull String token);

    /**
     * 撤销用户的所有令牌。
     *
     * <p>使指定用户的所有令牌立即失效，通常用于强制下线场景。
     *
     * @param userId 用户 ID，永不为 null
     */
    void invalidateAllTokens(@NonNull String userId);

    /**
     * 撤销刷新令牌。
     *
     * <p>在 Token 刷新时调用，防止旧的 Refresh Token 被重复使用。
     *
     * @param refreshToken 刷新令牌，永不为 null
     * @since 1.1.0
     */
    default void invalidateRefreshToken(@NonNull String refreshToken) {
        // 默认不实现，子类可覆盖
    }

    /**
     * 获取访问令牌的 TTL（生存时间）。
     *
     * @return 访问令牌的 TTL，单位为秒
     */
    long getAccessTokenTtl();

    /**
     * 获取刷新令牌的 TTL（生存时间）。
     *
     * @return 刷新令牌的 TTL，单位为秒
     */
    long getRefreshTokenTtl();
}
